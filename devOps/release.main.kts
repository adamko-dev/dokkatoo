#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:3.5.2")
@file:DependsOn("me.alllex.parsus:parsus-jvm:0.4.0")

import Release_main.SemVer.Companion.SemVer
import com.github.ajalt.clikt.core.CliktCommand
import java.io.File
import java.lang.ProcessBuilder.Redirect.PIPE
import kotlin.system.exitProcess
import me.alllex.parsus.parser.*
import me.alllex.parsus.token.literalToken
import me.alllex.parsus.token.regexToken

try {
  Release.main(args)
  exitProcess(0)
} catch (ex: Exception) {
  println("${ex::class.simpleName}: ${ex.message}")
  exitProcess(1)
}

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

  override fun run() {

    //region Validation
    check(git.status().isEmpty()) {
      "git repo is not clean. Stash or commit changes before making a release."
    }
    check(currentVersion.snapshot) {
      "Current version must be a SNAPSHOT, but was $currentVersion"
    }
    val startBranch = git.currentBranch()
    check(startBranch == "main") {
      "Must be on the main branch to make a release, but current branch is $startBranch"
    }
    gh.setRepo("adamko-dev/dokkatoo")
    //endregion

    echo("Current version is $currentVersion")
    echo("git dir is ${git.rootDir}")

    val releaseVersion = prompt(
      text = "version to release?",
      default = currentVersion.copy(snapshot = false).toString(),
      requireConfirmation = true,
    ) {
      SemVer.of(it)
    } ?: error("invalid SemVer")

    check(!releaseVersion.snapshot) {
      "versionToRelease must not be a snapshot version, but was $releaseVersion"
    }

    val nextVersion = prompt(
      text = "post-release version?",
      default = releaseVersion.incrementMinor(snapshot = true).toString(),
      requireConfirmation = true,
    ) {
      SemVer.of(it)
    } ?: error("invalid SemVer")

    echo("Current version is $currentVersion")
    confirm("Release $releaseVersion and bump to $nextVersion?", abort = true)

    updateAndRelease(releaseVersion)

    // Tag the release
    git.checkout(startBranch)
    git.pull(startBranch)
    require(currentVersion == releaseVersion) {
      "incorrect version after update. Expected $releaseVersion but got $currentVersion"
    }
    val tagName = "v$releaseVersion"
    git.tag(tagName)
    confirm("Push tag $tagName?", abort = true)
    git.push(tagName)
    echo("Tag pushed")

    // Bump the version to the next snapshot
    updateAndRelease(nextVersion)

    // Go back and pull the changes
    git.checkout(startBranch)
    git.pull(startBranch)

    echo("Released version $releaseVersion")
  }

  private fun updateAndRelease(version: SemVer) {
    // checkout a release branch
    val releaseBranch = "release/v$version"
    echo("checkout out new branch...")
    git.checkout(releaseBranch)

    // update the version & run tests
    currentVersion = version
    echo("running Gradle check...")
    gradle.check()

    // commit and push
    echo("committing...")
    git.commit("release $version")
    echo("pushing...")
    git.push(releaseBranch)

    // create a new PR
    echo("creating PR...")
    gh.createPr(releaseBranch)

    confirm("Merge the PR for branch $releaseBranch?", abort = true)
    mergeAndWait(releaseBranch)
    echo("$releaseBranch PR merged")
  }

  /** git commands */
  private val git = object {
    val rootDir = File(runCommand("git rev-parse --show-toplevel", dir = null))
    fun checkout(branch: String): String = runCommand("git checkout -b $branch")
    fun commit(message: String): String = runCommand("git commit -a -m \"$message\"")
    fun currentBranch(): String = runCommand("git symbolic-ref --short HEAD")
    fun pull(ref: String): String = runCommand("git pull origin $ref")
    fun push(ref: String): String = runCommand("git push origin $ref")
    fun status(): String {
      runCommand("git fetch --all")
      return runCommand("git status --porcelain=v2")
    }

    fun tag(tag: String): String {
      return runCommand("git tag $tag")
    }
  }

  /** GitHub commands */
  private val gh = object {
    fun setRepo(repo: String): String =
      runCommand("gh repo set-default $repo")

    fun prState(branchName: String): String =
      runCommand("gh pr view $branchName --json state --jq .state")

    fun createPr(branch: String): String =
      runCommand("gh pr create --head $branch --fill")

    fun autoMergePr(branch: String): String =
      runCommand("gh pr merge $branch --squash --auto --delete-branch")

    fun waitForPrChecks(branch: String): String =
      runCommand("gh pr checks $branch --watch --interval 30")
  }

  /** GitHub commands */
  private val gradle = object {
    fun check(): String = runCommand("./gradlew check --no-daemon")
  }

  //  private val currentDir: String get() = System.getProperty("user.dir")

  private val buildGradleKts: File by lazy {
    val rootDir = git.rootDir
    File("$rootDir/build.gradle.kts").apply {
      require(exists()) { "could not find build.gradle.kts in $rootDir" }
    }
  }

  private fun runCommand(
    cmd: String,
    dir: File? = git.rootDir,
  ): String {
    val args = parseSpaceSeparatedArgs(cmd)

    val process = ProcessBuilder(*args.toTypedArray()).apply {
      redirectOutput(PIPE)
      redirectError(PIPE)
      if (dir != null) directory(dir)
    }.start()

    val ret = process.waitFor()

    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (ret != 0) {
      error("command '$cmd' failed:\n$output")
    }

    return output.trim()
  }

  /** Read/write the version set in the root `build.gradle.kts` file */
  private var currentVersion: SemVer
    get() {
      val versionLine = buildGradleKts.useLines { lines ->
        lines.firstOrNull { it.startsWith("version = ") }
      }

      requireNotNull(versionLine) { "cannot find version in $buildGradleKts" }

      val rawVersion = versionLine.substringAfter("\"").substringBefore("\"")

      return SemVer(rawVersion)
    }
    set(value) {
      val updatedFile = buildGradleKts.useLines { lines ->
        lines.joinToString(separator = "\n", postfix = "\n") { line ->
          if (line.startsWith("version = ")) {
            "version = \"${value}\""
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

private data class SemVer(
  val major: Int,
  val minor: Int,
  val patch: Int,
  val snapshot: Boolean,
) {

  fun incrementMinor(snapshot: Boolean): SemVer =
    copy(minor = minor + 1, snapshot = snapshot)

  override fun toString(): String =
    "$major.$minor.$patch" + if (snapshot) "-SNAPSHOT" else ""

  companion object {
    fun SemVer(input: String): SemVer =
      SemVerParser.parseEntire(input).getOrElse { error ->
        error("provided version to release must be SemVer X.Y.Z, but got error while parsing: $error")
      }

    fun of(input: String): SemVer? =
      SemVerParser.parseEntire(input).getOrElse { return null }

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

    private val snapshot by -dashSeparator * literalToken("SNAPSHOT")

    override val root: Parser<SemVer> by parser {
      val major = number()
      dotSeparator()
      val minor = number()
      dotSeparator()
      val patch = number()
      val snapshot = checkPresent(snapshot)
      SemVer(
        major = major,
        minor = minor,
        patch = patch,
        snapshot = snapshot,
      )
    }
  }
}
