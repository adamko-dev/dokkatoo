package dev.adamko.dokkatoo.dokka.parameters

import org.jetbrains.dokka.Platform


/**
 * @see org.jetbrains.dokka.Platform
 */
@Suppress("EnumEntryName") // use lowercase names to match Dokka
enum class KotlinPlatform(
  internal val dokkaType: Platform
) {
  jvm(Platform.jvm),
  js(Platform.js),
  wasm(Platform.wasm),
  native(Platform.native),
  common(Platform.common),
  ;

  val key: String by dokkaType::key

  companion object {
    internal val entries: Set<KotlinPlatform> = values().toSet()

    val DEFAULT: KotlinPlatform = entries.first { it.dokkaType == Platform.DEFAULT }

    fun fromString(key: String): KotlinPlatform {
      return when (key.toLowerCase()) {
        jvm.key, "androidjvm", "android" -> jvm
        js.key                           -> js
        wasm.key                         -> wasm
        native.key                       -> native
        common.key, "metadata"           -> common
        else                             -> error("Unrecognized platform: $key")
      }
    }
  }
}
