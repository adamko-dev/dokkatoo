package dev.adamko.dokkatoo.tasks

//import dev.adamko.dokkatoo.internal.DokkatooInternalApi
//import javax.inject.Inject
//import kotlinx.serialization.KSerializer
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.SerializationStrategy
//import kotlinx.serialization.descriptors.PrimitiveKind
//import kotlinx.serialization.descriptors.SerialDescriptor
//import kotlinx.serialization.descriptors.elementDescriptors
//import kotlinx.serialization.descriptors.elementNames
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//import kotlinx.serialization.json.*
//import kotlinx.serialization.serializer
//import org.gradle.api.file.ConfigurableFileCollection
//import org.gradle.api.file.DirectoryProperty
//import org.gradle.api.file.FileSystemOperations
//import org.gradle.api.file.RegularFileProperty
//import org.gradle.api.tasks.*
//import org.gradle.api.tasks.PathSensitivity.RELATIVE
//
///**
// * Prepare a Dokka Module for aggregation into a Dokka Publication.
// *
// * This task is only needed to work around Gradle bugs and deficiencies that prevent
// * sensible sharing of files between subprojects.
// */
//@CacheableTask
//abstract class ConsolidateDokkaModuleElementsTask
//@DokkatooInternalApi
//@Inject
//constructor(
//  private val fs: FileSystemOperations,
//) : DokkatooTask() {
//
//  @get:OutputDirectory
//  abstract val outputDirectory: DirectoryProperty
//
//  @get:InputDirectory
//  @get:PathSensitive(RELATIVE)
//  abstract val moduleDirectory: DirectoryProperty
//
//  @get:InputFiles
//  @get:Optional
//  @get:PathSensitive(RELATIVE)
//  abstract val includes: ConfigurableFileCollection
//
//  @get:InputFiles
//  @get:Optional
//  @get:PathSensitive(RELATIVE)
//  abstract val moduleDescriptor: RegularFileProperty
//
//  @TaskAction
//  internal fun createModuleDescriptor() {
//    fs.sync {
//      into(outputDirectory)
//
//      from(moduleDescriptor)
//
//      from(moduleDirectory) {
//        into("module")
//      }
//
//      from(includes) {
//        into("includes")
//      }
//    }
//  }
//}
