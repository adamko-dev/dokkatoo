package dev.adamko.dokkatoo.utils

import java.io.File

fun File.copyInto(directory: File, overwrite: Boolean = false) =
  copyTo(directory.resolve(name), overwrite = overwrite)
