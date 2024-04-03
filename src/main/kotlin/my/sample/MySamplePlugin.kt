package my.sample

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
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
        Unit
    }
}