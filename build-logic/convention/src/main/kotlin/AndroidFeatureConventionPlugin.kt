import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.dependencies

/**
 * A module of Prumo (`:feature:<id>`): an Android library with Compose and Hilt, the dependencies
 * of the `PrumoFeature` contract, and the rule that keeps modules independent: a feature never
 * depends on another feature (integrations between modules are the server's, through events).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("prumo.android.library")
        pluginManager.apply("prumo.android.compose")
        pluginManager.apply("prumo.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            add("implementation", project(":core:feature"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:network"))
            add("implementation", project(":core:model"))
            add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
            add("testImplementation", project(":core:testing"))
        }
        val self = path
        configurations.configureEach {
            dependencies.configureEach {
                val dependency = this
                if (dependency is ProjectDependency && dependency.path.startsWith(":feature:") &&
                    dependency.path != self
                ) {
                    throw GradleException(
                        "$self depends on ${dependency.path}: a feature module must not depend on another feature " +
                            "module (docs/arquitetura.md, \"Regras de dependência\").",
                    )
                }
            }
        }
    }
}
