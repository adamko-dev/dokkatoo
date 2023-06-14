#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:3.5.2")
@file:DependsOn("me.alllex.parsus:parsus-jvm:0.4.0")

import Release_main.SemVer.Companion.SemVer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import java.io.File
import java.lang.ProcessBuilder.Redirect.INHERIT
import me.alllex.parsus.parser.*
import me.alllex.parsus.token.literalToken
import me.alllex.parsus.token.regexToken

/**
 * Release a new version.
 *
 * Requires:
 * * [gh cli](https://cli.github.com/manual/gh)
 * * [kotlin](https://kotlinlang.org/docs/command-line.html)
 * * [git](https://git-scm.com/)
 */
// based on https://github.com/apollographql/apollo-kotlin/blob/v4.0.0-dev.2/scripts/release.main.kts
object Release : CliktCommand() {
  private val versionToRelease by option(help = "version to release")
    .prompt("versionToRelease")

  override fun run() {
    val versionToRelease = SemVer(versionToRelease)
    val nextVersion = versionToRelease.copy(
      minor = versionToRelease.minor + 1,
      snapshot = true
    )

    //region Validation
//    check(currentDir() == git.rootDir()) {
//      "must run release.main.kts in the root "
//    }
    check(!versionToRelease.snapshot) {
      "versionToRelease must not be a snapshot version, but was $versionToRelease"
    }

    check(git.status().isEmpty()) {
      "git repo is not clean. Stash or commit changes before making a release."
    }
    check(getCurrentVersion().snapshot) {
      "Current version must be a SNAPSHOT, but was ${getCurrentVersion()}"
    }

    val startBranch = git.currentBranch()
    check(startBranch == "main") {
      "Must be on the main branch to make a release, but current branch is $startBranch"
    }
    //endregion

    echo("Current version is ${getCurrentVersion()}")
    confirm("Release $versionToRelease and bump to $nextVersion?", abort = true)

    updateAndRelease(versionToRelease)

    // Tag the release
    git.checkout(startBranch)
    git.pull(startBranch)
    require(getCurrentVersion() == versionToRelease) {
      "incorrect version after update. Expected $versionToRelease but got ${getCurrentVersion()}"
    }
    val tagName = "v$versionToRelease"
    git.tag(tagName)
    confirm("Push tag $tagName?", abort = true)
    git.push(tagName)
    echo("Tag pushed")

    // Bump the version to the next snapshot
    updateAndRelease(nextVersion)

    // Go back and pull the changes
    git.checkout(startBranch)
    git.pull(startBranch)

    echo("Released version $versionToRelease")
  }

  private fun updateAndRelease(version: SemVer) {
    // checkout a release branch
    val releaseBranch = "release-$version"
    git.checkout(releaseBranch)

    // update the version & run tests
    setCurrentVersion(version)
    gradle.check()

    // commit and push
    git.commit("release $version")
    git.push(releaseBranch)

    // create a new PR
    gh.createPr(releaseBranch)

    confirm("Merge the PR for branch $releaseBranch?", abort = true)
    mergeAndWait(releaseBranch)
    echo("$releaseBranch PR merged")
  }

  /** git commands */
  private val git = object {
    fun checkout(branch: String): String = runCommand("git checkout -b $branch")
    fun commit(message: String): String = runCommand("git commit -a -m \"$message\"")
    fun currentBranch(): String = runCommand("git symbolic-ref --short HEAD")
    fun pull(ref: String): String = runCommand("git pull origin $ref")
    fun push(ref: String): String = runCommand("git push origin $ref")
    fun rootDir(): String = runCommand("git rev-parse --show-toplevel")
    fun status(): String = runCommand("git status --porcelain=v2")
    fun tag(tag: String): String = runCommand("git tag $tag")
  }

  /** GitHub commands */
  private val gh = object {
    fun prState(branchName: String): String =
      runCommand("gh pr view $branchName --json state --jq .state")

    fun createPr(branch: String): String =
      runCommand("gh pr create --base $branch --fill")

    fun autoMergePr(branch: String): String =
      runCommand("gh pr merge $branch --squash --auto --delete-branch")

    fun waitForPrChecks(branch: String): String =
      runCommand("gh pr checks $branch --watch --interval 30")
  }

  /** GitHub commands */
  private val gradle = object {
    fun check(): String = runCommand("gradlew check")
  }

  //  private val currentDir: String get() = System.getProperty("user.dir")

  private val buildGradleKts: File by lazy {
    File(git.rootDir()).resolve("build.gradle.kts").also {
      require(it.exists()) { "could not find build.gradle.kts in ${git.rootDir()}" }
    }
  }

  private fun runCommand(cmd: String): String {
    val args = parseSpaceSeparatedArgs(cmd)

    val process = ProcessBuilder(*args.toTypedArray())
      .redirectError(INHERIT)
      .start()

    val ret = process.waitFor()

    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (ret != 0) {
      error("command '$cmd' failed:\n$output")
    }

    return output.trim()
  }

  private fun getCurrentVersion(): SemVer {
    val versionLine = buildGradleKts.useLines { lines ->
      lines.firstOrNull { it.startsWith("version = ") }
    }

    requireNotNull(versionLine) { "cannot find version in $buildGradleKts" }

    val rawVersion = versionLine.substringAfter("\"").substringBefore("\"")

    return SemVer(rawVersion)
  }

  private fun setCurrentVersion(newVersion: SemVer) {
    val updatedFile = buildGradleKts.useLines { lines ->
      lines.joinToString("\n") { line ->
        if (line.startsWith("version = ")) {
          "version = \"${newVersion}\""
        } else {
          line
        }
      }
    }
    buildGradleKts.writeText(updatedFile)
  }

  private fun mergeAndWait(branchName: String) {
    gh.autoMergePr(branchName)
    echo("Waiting for the PR to be merged...")
    while (gh.prState(branchName) != "MERGED") {
      Thread.sleep(1_000)
      echo(".", trailingNewline = false)
    }
  }

  private fun parseSpaceSeparatedArgs(argsString: String): List<String> {
    val parsedArgs = mutableListOf<String>()
    var inQuotes = false
    var currentCharSequence = StringBuilder()
    fun saveArg(wasInQuotes: Boolean) {
      if (wasInQuotes || currentCharSequence.isNotBlank()) {
        parsedArgs.add(currentCharSequence.toString())
        currentCharSequence = StringBuilder()
      }
    }
    argsString.forEach { char ->
      if (char == '"') {
        inQuotes = !inQuotes
        // Save value which was in quotes.
        if (!inQuotes) {
          saveArg(true)
        }
      } else if (char.isWhitespace() && !inQuotes) {
        // Space is separator
        saveArg(false)
      } else {
        currentCharSequence.append(char)
      }
    }
    if (inQuotes) {
      error("No close-quote was found in $currentCharSequence.")
    }
    saveArg(false)
    return parsedArgs
  }

}


Release.main(args)


private data class SemVer(
  val major: Int,
  val minor: Int,
  val patch: Int,
  val snapshot: Boolean,
) {

  override fun toString(): String =
    "$major.$minor.$patch" + if (snapshot) "-SNAPSHOT" else ""

  companion object {
    fun SemVer(input: String): SemVer =
      SemVerParser.parseEntire(input).getOrElse { error ->
        error("provided version to release must be SemVer X.Y.Z, but got error while parsing: $error")
      }

    fun isValid(input: String): Boolean =
      try {
        SemVerParser.parseEntireOrThrow(input)
        true
      } catch (ex: ParseException) {
        false
      }
  }

  private object SemVerParser : Grammar<SemVer>() {
    private val dotSeparator by literalToken(".")
    private val dashSeparator by literalToken("-")

    /** Non-negative number that is either 0, or does not start with 0 */
    private val number: Parser<Int> by regexToken("""0|[1-9]\d*""").map { it.text.toInt() }

    private val metadata by optional(-dashSeparator * regexToken(""".+"""))
      .map { it?.text ?: "" }

    override val root: Parser<SemVer> by parser {
      val major = number()
      dotSeparator()
      val minor = number()
      dotSeparator()
      val patch = number()
      val metadata = metadata()
      SemVer(
        major = major,
        minor = minor,
        patch = patch,
        snapshot = metadata == "SNAPSHOT",
      )
    }
  }
}
