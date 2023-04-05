import org.gradle.kotlin.dsl.provider.PrecompiledScriptPluginsSupport
import org.gradle.kotlin.dsl.provider.gradleKotlinDslJarsOf
import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`
  `java-test-fixtures`
}



dependencies {
  implementation(libs.gradlePlugin.kotlin)
  implementation(projects.modules.dokkatooPlugin)
  testFixturesImplementation(libs.gradlePlugin.kotlin)
  testFixturesImplementation(projects.modules.dokkatooPlugin)

  implementation(gradleApi())
  implementation(gradleTestKit())
  implementation(gradleKotlinDsl())
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")

  testFixturesImplementation(gradleApi())
  testFixturesImplementation(gradleTestKit())
  testFixturesImplementation(gradleKotlinDsl())

  kotlinCompilerPluginClasspathTestFixtures(gradleKotlinDslJarsOf(project))
  kotlinCompilerPluginClasspathTestFixtures(gradleApi())
  kotlinCompilerPluginClasspathTestFixtures(gradleKotlinDsl())

  testFixturesImplementation(gradleKotlinDslJarsOf(project))
  testFixturesImplementation(gradleApi())
  testFixturesImplementation(gradleKotlinDsl())
  testFixturesImplementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")

  testFixturesKotlinScriptDef(gradleKotlinDslJarsOf(project))
  testFixturesKotlinScriptDef(gradleApi())
  testFixturesKotlinScriptDef(gradleKotlinDsl())

}

tasks.compileTestFixturesKotlin

fun pcspTarget(
  project: Project,
  sourceSet: Provider<KotlinSourceSet>,
) =
  object : PrecompiledScriptPluginsSupport.Target {

    override val project: Project = project

    override val kotlinSourceDirectorySet: SourceDirectorySet
      get() = sourceSet.get().kotlin

    @Suppress("OVERRIDE_DEPRECATION")
    override val kotlinCompileTask: TaskProvider<out Task>
      get() = error("No longer used")

    @Suppress("OVERRIDE_DEPRECATION")
    override fun applyKotlinCompilerArgs(args: List<String>) =
      error("No longer used.")
  }

serviceOf<PrecompiledScriptPluginsSupport>().enableOn(
  pcspTarget(project, kotlin.sourceSets.testFixtures)
)

dependencies {
}

configurations.kotlinCompilerPluginClasspathTestFixtures {
  extendsFrom(configurations.kotlinCompilerPluginClasspath.get())
  extendsFrom(configurations.embeddedKotlin.get())
}

configurations.kotlinCompilerPluginClasspathTestFixtures.configure {
  extendsFrom(configurations.embeddedKotlin.get())
}
configurations.testFixturesCompileOnly.configure {
  extendsFrom(configurations.embeddedKotlin.get())
}
//configurations.testFixturesImplementation.configure {
//  extendsFrom(configurations.getByName("embeddedKotlin"))
//}
