import com.android.build.api.dsl.LibraryExtension
import io.github.themeshmaker.dunya.buildlogic.DeenBuild
import io.github.themeshmaker.dunya.buildlogic.lib
import io.github.themeshmaker.dunya.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = DeenBuild.COMPILE_SDK
                defaultConfig {
                    minSdk = DeenBuild.MIN_SDK
                }
                compileOptions {
                    sourceCompatibility = DeenBuild.JAVA_VERSION
                    targetCompatibility = DeenBuild.JAVA_VERSION
                }
            }

            dependencies {
                add("testImplementation", libs.lib("junit"))
            }
        }
    }
}
