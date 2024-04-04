package my.sample

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.text.SimpleDateFormat
import java.util.*

class MySamplePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        tasks.register<MySampleTask>(name = "mySampleTask") {
            group = "sample"
            description = "A sample task"
            input.set(SimpleDateFormat("yyyy-MM-dd").format(Date()))
            output.set(layout.buildDirectory.file("mySampleTaskOutput.txt"))
        }

        // dummy inlined code
        tasks.withType<Test> {
             the<JacocoTaskExtension>().apply {
                 logger.lifecycle("JacocoTaskExtension: $this")
             }
        }
        Unit
    }
}