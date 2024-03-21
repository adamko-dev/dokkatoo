package dev.adamko.dokkatoo.utils

import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun Path.shouldHaveSameStructureAs(path: Path, skipEmptyDirs: Boolean) {
  if (skipEmptyDirs) {
    toFile().shouldHaveSameStructureAs(path.toFile(), ::isEmptyDir, ::isEmptyDir)
  } else {
    toFile().shouldHaveSameStructureAs(path.toFile())
  }
}

fun Path.shouldHaveSameStructureAndContentAs(path: Path, skipEmptyDirs: Boolean) {
  if (skipEmptyDirs) {
    toFile().shouldHaveSameStructureAndContentAs(path.toFile(), ::isEmptyDir, ::isEmptyDir)
  } else {
    toFile().shouldHaveSameStructureAndContentAs(path.toFile())
  }
}

private fun isEmptyDir(file: File): Boolean =
  file.isDirectory && Files.newDirectoryStream(file.toPath()).use { it.count() } == 0
