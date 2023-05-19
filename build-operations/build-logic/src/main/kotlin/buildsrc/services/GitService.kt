package buildsrc.services

import java.io.File
import javax.inject.Inject
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOutput
import buildsrc.utils.parseSpaceSeparatedArgs

/**
 * Execute Git commands on the local machine.
 */
abstract class GitService @Inject constructor(
  private val providers: ProviderFactory,
) : BuildService<GitService.Params> {

  private val logger: Logger = Logging.getLogger(GitService::class.java)

  interface Params : BuildServiceParameters {
    /**
     * The full path of the git executable.
     *
     * If `git` is already on `PATH`, then the value can just be `git`
     *
     * @see dev.adamko.vcsmvnpub.VcsMvnPublishSettings.gitExec
     */
    val gitExec: Property<String>

    /** The default value of an origin repo, if required by a Git command. */
    val defaultOrigin: Property<String>

    val logLevel: Property<LogLevel>

    /** Set `--git-dir` for each command, to ensure each command is run in the correct repo. */
    val gitDirFlagEnabled: Property<Boolean>
  }


  fun isInsideWorkTree(
    repoDir: File,
  ): Provider<Boolean> = runCatching {
    if (!repoDir.resolve(".git").exists()) {
      providers.provider { false }
    } else {
      val result = repoDir git "rev-parse --is-inside-work-tree"
      result.standardOutput.asText.map { it.trim().toBoolean() }
    }
  }.getOrElse { providers.provider { false } }


  fun topLevelDir(
    repoDir: File,
  ): Provider<File> {
    val topLevel = repoDir git "rev-parse --show-toplevel"
    return topLevel.standardOutput.asText.map {
      File(it.trim())
    }
  }


  fun fetch(
    repoDir: File,
    origin: String? = defaultOrigin,
    depth: Int? = null,
  ): String {
    val depthFlag = if (depth != null) "--depth $depth" else ""
    val result = repoDir git "fetch $depthFlag $origin"
    return result.getAndLog()
  }


  fun init(
    repoDir: File,
    branch: String? = null,
  ): String {
    val initialBranch = if (branch.isNullOrBlank()) "" else "--initial-branch=$branch"
    val result = repoDir git "init ${repoDir.canonicalPath} $initialBranch"
    return result.getAndLog()
  }


  fun status(
    repoDir: File,
    branch: Boolean = false,
    porcelain: Boolean = false,
  ): String {
    val branchFlag = if (branch) "--branch" else ""
    val porcelainFlag = if (porcelain) "--porcelain" else ""

    return (repoDir git "status $porcelainFlag $branchFlag").getAndLog()
  }


  // https://git-scm.com/docs/git-remote#Documentation/git-remote.txt-emaddem
  fun remoteAdd(
    repoDir: File,
    remoteUri: String,
    branch: String? = null,
    origin: String? = defaultOrigin,
    fetch: Boolean = false,
  ): String {
    val branchTrack = if (branch.isNullOrBlank()) "" else "-t $branch"
    val fetchFlag = if (fetch) "-f" else ""

    val result = repoDir git "remote add $fetchFlag $branchTrack $origin $remoteUri"
    return result.getAndLog()
  }


  // https://git-scm.com/docs/git-checkout#Documentation/git-checkout
  fun checkout(
    repoDir: File,
    branch: String,
    origin: String? = defaultOrigin,
    force: Boolean = false,
  ): String {
    val trackFlag = when {
      origin.isNullOrBlank() -> "      -b $branch"
      else                   -> " --track $origin/$branch"
      //                         track without -b implies creation, git will guess branch name
    }.trim()

    val forceFlag = if (force) "--force" else ""

    val result = repoDir git "checkout $trackFlag $forceFlag ."
    return result.getAndLog()
  }


  // https://git-scm.com/docs/git-checkout#Documentation/git-checkout
  fun checkoutOrphan(
    repoDir: File,
    branch: String,
    force: Boolean = false,
  ): String {
    val forceFlag = if (force) "--force" else ""

    val result = repoDir git "checkout --orphan $branch $forceFlag"
    return result.getAndLog()
  }

  /** Switch to another branch, potentially creating it */
  fun switch(
    repoDir: File,
//    origin: String? = defaultOrigin,
    branch: String,
  ): String {
    val result = repoDir git "switch $branch"
    return result.getAndLog()
  }


  fun clean(
    repoDir: File,
    force: Boolean,
    directories: Boolean,
  ): String {
    val forceFlag = if (force) "--force" else ""
    val directoriesFlag = if (directories) "-d" else ""

    val result = repoDir git "clean $forceFlag $directoriesFlag"
    return result.getAndLog()
  }


  /**
   * @param[message] The raw message. Line breaks and quotes `"` will be escaped.
   */
  fun commit(
    repoDir: File,
    message: String,
    allowEmpty: Boolean = false,
  ): String {
    require(message.isNotBlank()) { "commit message must not be blank" }

    val escapedMessage = message
      .trim()
      .replace('\"', '\'')
      .lines()
      .joinToString(separator = " ") { line -> "-m \"${line}\"" }

    val allowEmptyFlag = if (allowEmpty) "--allow-empty" else ""

    return (repoDir git "commit $allowEmptyFlag $escapedMessage").getAndLog()
  }


  fun addAll(
    repoDir: File,
  ): String = (repoDir git "add --all").getAndLog()


  fun branchTrackRemote(
    repoDir: File,
    origin: String? = defaultOrigin,
    branch: String
  ): String {
    return (repoDir git "branch --track $origin/$branch").getAndLog()
  }


  // https://git-scm.com/docs/git-push
  fun push(
    repoDir: File,
    origin: String? = defaultOrigin,
    head: Boolean = true,
  ): String {
    val headFlag = if (head) "HEAD" else ""

    return (repoDir git "push $origin $headFlag").getAndLog()
  }


  private fun configGet(
    repoDir: File,
    property: String,
  ): Provider<String> = providers.provider {
    runCatching {
      val result = repoDir git "config --get $property"
      result.standardOutput.asText.map { it.trim() }.get()
    }.getOrElse { "" }
  }


  fun configGetRemoteOriginUrl(
    repoDir: File,
    origin: String? = defaultOrigin,
  ): Provider<String> = runCatching {
    configGet(repoDir, "remote.${origin}.url").orElse("")
  }.getOrElse {
    logger.error("error getting remote origin URL", it)
    providers.provider { "" }
  }


  fun doesBranchExistOnRemote(
    repoDir: File,
    branch: String,
    origin: String? = defaultOrigin,
  ): Provider<Boolean> {
    val result = repoDir git "ls-remote --heads $origin $branch"
    return result.standardOutput.asText.map { it.isNotBlank() }
  }


  /**
   * Determine the current branch by parsing the branch status.
   *
   * Branch status example:
   *
   * ```shell
   * > git status --branch --porcelain=v2
   * # branch.oid eb2eb732933f0e93d7cc4ad470d6ee2e95832979
   * # branch.head artifacts
   * # branch.upstream origin/artifacts
   * # branch.ab +0 -0
   * ```
   */
  fun getCurrentBranch(
    repoDir: File,
  ): Provider<String> = runCatching {
    val result = repoDir git "status --branch --porcelain=v2"
    result.standardOutput.asText.map { output ->
      output.lines()
        .firstOrNull { "branch.head" in it }
        ?.substringAfter("branch.head")
        ?.trim() ?: ""
    }
  }.getOrElse { providers.provider { "" } }


  /** Execute a Git command in the provided directory */
  private infix fun File.git(
    cmd: String
  ): ExecOutput {
    if (!exists()) {
      // prevent unhelpful error https://github.com/gradle/gradle/issues/21007
      error("attempted to execute git command, but directory did not exist $canonicalPath")
    }

    val gitDirFlag = if (gitDirFlagEnabled) "--git-dir=$canonicalPath/.git" else ""

    val cmdParsed = parseSpaceSeparatedArgs("$gitExec $gitDirFlag $cmd")

    logger.lifecycle("GitService git exec $canonicalPath $cmdParsed")
    return providers.exec {
      workingDir(canonicalPath)
      commandLine(cmdParsed)
    }
  }


  private fun ExecOutput.getAndLog(): String {
    val result = runCatching { result.orNull }.getOrNull()

    val stdOut = runCatching { standardOutput.asText.orNull }.getOrNull()
    val stdErr = runCatching { standardError.asText.orNull }.getOrNull()

    val outputFormatted = (stdOut ?: stdErr ?: "null").prependIndent("    ║ ")

    logger.log(
      logLevel,
      """
        |---
        |  git exec [${result?.exitValue}]
        |  result: 
        |$outputFormatted
        |---  
      """.trimMargin()
    )
    return stdOut ?: stdErr ?: ""
  }


  private val gitExec: String
    get() = parameters.gitExec.get()

  private val defaultOrigin: String?
    get() = parameters.defaultOrigin.orNull

  private val logLevel: LogLevel
    get() = parameters.logLevel.orNull ?: LogLevel.LIFECYCLE

  private val gitDirFlagEnabled: Boolean
    get() = parameters.gitDirFlagEnabled.getOrElse(false)

  companion object {
    const val NAME: String = "GitService"
  }
}
