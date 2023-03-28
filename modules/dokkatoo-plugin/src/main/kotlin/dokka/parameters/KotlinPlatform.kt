package dev.adamko.dokkatoo.dokka.parameters

import org.jetbrains.dokka.Platform


/**
 * The Kotlin
 *
 * @see org.jetbrains.dokka.Platform
 */
enum class KotlinPlatform {
  JVM,
  JS,
  WASM,
  Native,
  Common,
  ;

  val key: String = name.toLowerCase()

  companion object {
    internal val entries: Set<KotlinPlatform> = values().toSet()

    val DEFAULT: KotlinPlatform = JVM

    fun fromString(key: String): KotlinPlatform {

      return when (key.toLowerCase()) {
        JVM.key, "androidjvm", "android" -> JVM
        JS.key                           -> JS
        WASM.key                         -> WASM
        Native.key                       -> Native
        Common.key, "metadata"           -> Common
        else                             -> error("Unrecognized platform: $key")
      }
    }

    // Not defined as a property to try and minimize the dependency on Dokka Core types
    internal val KotlinPlatform.dokkaType: Platform
      get() =
        when (this) {
          JVM    -> Platform.jvm
          JS     -> Platform.js
          WASM   -> Platform.wasm
          Native -> Platform.native
          Common -> Platform.common
        }
  }
}
