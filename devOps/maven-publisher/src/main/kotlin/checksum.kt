import java.security.MessageDigest
import okio.BufferedSource
import okio.ByteString.Companion.toByteString

internal fun BufferedSource.md5() = digest("MD5")
internal fun BufferedSource.sha1() = digest("SHA1")


private fun BufferedSource.digest(name: String): String {
  val md = MessageDigest.getInstance(name)

  val scratch = ByteArray(1024)
  var read: Int = read(scratch)
  while (read > 0) {
    md.update(scratch, 0, read)
    read = read(scratch)
  }

  val digest = md.digest()

  return digest.toByteString().hex()
}
