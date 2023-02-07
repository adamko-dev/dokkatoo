package buildsrc.conventions.buildsrc.conventions

import gradle.kotlin.dsl.accessors._1aa7e5c26223e2b5ce19d03a79e08c0e.java
import gradle.kotlin.dsl.accessors._1aa7e5c26223e2b5ce19d03a79e08c0e.pluginDescriptors
import gradle.kotlin.dsl.accessors._1aa7e5c26223e2b5ce19d03a79e08c0e.sourceSets
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`java-gradle-plugin`
import org.gradle.kotlin.dsl.named

plugins {
  `java-gradle-plugin`
}

fun registerGradleVariant(name: String, gradleVersion: String) {
  val variantSources = sourceSets.create(name)

  java {
    registerFeature(variantSources.name) {
      usingSourceSet(variantSources)
//            capability("${project.group}", "${project.name}", "${project.version}")
      capability("org.jetbrains.dokka", "${project.name}", "2.0.0")

      withJavadocJar()
      withSourcesJar()
    }
  }

  configurations
    .matching { it.isCanBeConsumed && it.name.startsWith(variantSources.name) }
    .configureEach {
      attributes {
        attribute(GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(gradleVersion))
      }
    }

  tasks.named<Copy>(variantSources.processResourcesTaskName) {
    val copyPluginDescriptors = rootSpec.addChild()
    copyPluginDescriptors.into("META-INF/gradle-plugins")
//        copyPluginDescriptors.into(tasks.pluginDescriptors.flatMap { it.outputDirectory })
    copyPluginDescriptors.from(tasks.pluginDescriptors)
  }

  dependencies {
    add(variantSources.compileOnlyConfigurationName, gradleApi())
  }
}

//registerGradleVariant("gradle5", "5.0")
//registerGradleVariant("gradle6", "6.0")
registerGradleVariant("gradle7", "7.0")
registerGradleVariant("gradle8", "8.0")
