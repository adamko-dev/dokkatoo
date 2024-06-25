package dev.adamko.dokkatoo.utils


fun v(version: String): Version = Version(version)

fun Version(version: String): Version {
  val match = semverRegex.matchEntire(version) ?: error("Version $version does not match SemVer")
  val major = match.groups["major"]!!.value.toInt()
  val minor = match.groups["minor"]?.value?.toInt() ?: 0
  val patch = match.groups["patch"]?.value?.toInt() ?: 0
  return Version(major = major, minor = minor, patch = patch)
}

data class Version(
  val major: Int,
  val minor: Int,
  val patch: Int,
) : Comparable<Version> {

//  private val encoded: BigInteger
//
//  init {
//    val base = maxOf(major, minor, patch) + 1
//
//    "$patch".toBigInteger(base)
//    encoded = patch.toBigInteger() * base.pow(0) +
//        minor.toBigInteger() * base.pow(1) +
//        major.toBigInteger() * base.pow(2)
//  }

  override operator fun compareTo(other: Version): Int =
    when {
      major != other.major -> major.compareTo(other.major)
      minor != other.minor -> minor.compareTo(other.minor)
      patch != other.patch -> patch.compareTo(other.patch)
      else                 -> 0
    }

  operator fun compareTo(other: String): Int =
    compareTo(Version(other))

  override fun toString(): String =
    "$major.$minor.$patch"

  fun coerceAtLeast(min: String): Version = coerceAtLeast(v(min))

  companion object
}


// intentionally lenient because it's only for tests
private val semverRegex = Regex(
  """
    (?<major>\d+)\.(?<minor>\d+)\.?(?<patch>\d+)?[-+]?(?<tag>.*)?
  """.trimIndent()
)
