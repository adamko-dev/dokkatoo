package dev.adamko.dokkatoo

import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldExist
import java.io.File
import java.nio.file.Path

internal fun Path.shouldBeAnExistingFile(): Unit = run {
  shouldExist()
  shouldBeAFile()
}

internal fun File.shouldBeAnExistingFile(): Unit = run {
  shouldExist()
  shouldBeAFile()
}