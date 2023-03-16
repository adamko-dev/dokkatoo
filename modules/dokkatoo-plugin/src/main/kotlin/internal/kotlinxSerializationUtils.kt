package dev.adamko.dokkatoo.internal

import java.io.File
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add


@JvmName("addAllFiles")
internal fun JsonArrayBuilder.addAll(files: Iterable<File>) {
  files
    .map { it.canonicalFile.invariantSeparatorsPath }
    .forEach { path -> add(path) }
}

@JvmName("addAllStrings")
internal fun JsonArrayBuilder.addAll(values: Iterable<String>) {
  values.forEach { add(it) }
}

internal fun JsonArrayBuilder.addAllIfNotNull(values: Iterable<String>?) {
  values?.let(::addAll)
}

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: Boolean?) =
  value?.let { put(key, JsonPrimitive(it)) }

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: String?) =
  value?.let { put(key, JsonPrimitive(it)) }

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: File?) =
  value?.let { put(key, JsonPrimitive(it.canonicalFile.invariantSeparatorsPath)) }
