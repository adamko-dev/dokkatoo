import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library") version "4.2.2"
  kotlin("android") version "1.8.10"
  id("dev.adamko.dokkatoo") version "1.4.0-SNAPSHOT"
}


//val androidExt = project.extensions.getByType<BaseExtension>()
//@Suppress("DEPRECATION")
//val androidPlugin = project.plugins.findPlugin("com.android.library") as? BasePlugin ?: error("")
//
//val variants: DomainObjectSet<out BaseVariant> =
//  when (androidExt) {
//    is LibraryExtension -> androidExt.libraryVariants
//    is AppExtension     -> androidExt.applicationVariants
//    else                -> objects.domainObjectSet(BaseVariant::class)
//  }
//
//tasks.withType<KotlinCompile>().configureEach {
//  doLast {
//    println("libraries.asPath: ${libraries.asPath.split(":").joinToString("\n")}")
//  }
//}

android {
  defaultConfig {
    minSdkVersion(21)
    setCompileSdkVersion(29)
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.1.0")
}

dokkatoo {
//  versions.jetbrainsDokka.set("1.8.20-SNAPSHOT")
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml")
  }
}
