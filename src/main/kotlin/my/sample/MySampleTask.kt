package my.sample

import org.apache.commons.io.IOUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class MySampleTask : DefaultTask() {
    @get:Input
    abstract val input: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun run() {
        println(IOUtil::class)
        output.get().asFile.writeText(input.get())
    }
}