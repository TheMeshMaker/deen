import com.android.build.api.dsl.ApplicationExtension
import io.github.themeshmaker.dunya.buildlogic.DeenBuild
import io.github.themeshmaker.dunya.buildlogic.lib
import io.github.themeshmaker.dunya.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = DeenBuild.COMPILE_SDK
                defaultConfig {
                    minSdk = DeenBuild.MIN_SDK
                    targetSdk = DeenBuild.TARGET_SDK
                }
                compileOptions {
                    sourceCompatibility = DeenBuild.JAVA_VERSION
                    targetCompatibility = DeenBuild.JAVA_VERSION
                }
                // Local unit tests: android.* stubs (e.g. Log) return defaults instead of throwing.
                testOptions.unitTests.isReturnDefaultValues = true
            }

            dependencies {
                add("testImplementation", libs.lib("junit"))
                add("testImplementation", libs.lib("kotlinx-coroutines-test"))
                add("testImplementation", libs.lib("turbine"))
            }
        }
    }
}
