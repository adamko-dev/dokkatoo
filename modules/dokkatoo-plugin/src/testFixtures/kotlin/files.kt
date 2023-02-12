package dev.adamko.dokkatoo.utils

//import dev.adamko.dokkatoo.utils.FileCompareResult.Type.*
//import io.kotest.inspectors.forAll
//import io.kotest.matchers.collections.shouldBeEmpty
//import io.kotest.matchers.file.shouldExist
//import io.kotest.matchers.file.shouldHaveSameContentAs
//import io.kotest.matchers.file.shouldNotExist
import java.io.File

//import java.security.MessageDigest


fun File.copyInto(directory: File, overwrite: Boolean = false) =
  copyTo(directory.resolve(name), overwrite = overwrite)


//
//infix fun Path.shouldContainSameFilesAs(expected: Path) {
//  val compareResult = comparePaths(this, expected)
//
//  compareResult.forAll {
//    when (it.type) {
//      Unexpected      -> it.actual.shouldNotExist()
//      Missing         -> it.actual.shouldExist()
//      ContentMismatch -> it.actual.shouldHaveSameContentAs(it.expected)
//      TypeMismatch    -> it.actual
//    }
//  }
//
//  compareResult.shouldBeEmpty()
//}
//
//private fun comparePaths(rootActual: Path, rootExpected: Path): List<FileCompareResult> {
//
//  val actualChildPaths = rootActual.allRelativeChildPaths()
//  val expectedChildPaths = rootExpected.allRelativeChildPaths()
//
//  return (actualChildPaths + expectedChildPaths).mapNotNull { childPath ->
//    val expectedFile = rootActual.toFile().resolve(childPath)
//    val actualFile = rootExpected.toFile().resolve(childPath)
//
//    when {
//      expectedFile.exists() && actualFile.exists()  -> {
//        when {
//          expectedFile.isDirectory && actualFile.isDirectory ->
//            // don't compare directories, only files
//            null
//
//          expectedFile.isFile && actualFile.isFile           -> {
//            if (expectedFile.md5() != actualFile.md5()) {
//              FileCompareResult(
//                type = ContentMismatch,
//                expected = expectedFile,
//                actual = actualFile,
//              )
//            } else {
//              // the files match, the test passes
//              null
//            }
//          }
//
//          else                                               -> {
//            FileCompareResult(
//              type = TypeMismatch,
//              expected = expectedFile,
//              actual = actualFile,
//            )
//          }
//        }
//      }
//
//      !expectedFile.exists() && actualFile.exists() -> {
//        FileCompareResult(
//          type = Unexpected,
//          expected = expectedFile,
//          actual = actualFile,
//        )
//      }
//
//      expectedFile.exists() && !actualFile.exists() -> {
//        FileCompareResult(
//          type = Missing,
//          expected = expectedFile,
//          actual = actualFile,
//        )
//      }
//
//      // !expectedFile.exists() && !actualFile.exists()
//      else                                          ->
//        error("error while comparing files $expectedFile $actualFile")
//    }
//  }.toList()
//}
//
//private fun Path.allRelativeChildPaths() =
//  toFile().walk().map { it.toRelativeString(this.toFile()) }
//
//private data class FileCompareResult(
//  val type: Type,
//  val expected: File,
//  val actual: File,
//) {
//  enum class Type {
//    Unexpected,
//    Missing,
//    ContentMismatch,
//    TypeMismatch,
//  }
//}
//
//
//private fun Path.md5(): String = toFile().md5()
//
//
//private fun File.md5(): String =
//  MessageDigest.getInstance("MD5")
//    .apply { update(readBytes()) }
//    .digest()
//    .joinToString(separator = "") { byte -> "%02x".format(byte) }
