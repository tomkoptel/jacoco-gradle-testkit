import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata.IMPLEMENTATION_CLASSPATH_PROP_KEY
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata.METADATA_FILE_NAME
import org.jetbrains.kotlin.konan.file.use
import java.io.Serializable
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.Properties

plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("groovy-gradle-plugin")
    jacoco
}


val jacocoAgentJar by configurations.creating
jacocoAgentJar.isCanBeResolved = true

dependencies {
    compileOnly(gradleApi())
    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
    jacocoAgentJar("org.jacoco:org.jacoco.agent:0.8.12:runtime")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    val testRuns = layout.buildDirectory.dir("testRuns")
    systemProperty("testEnv.workDir", LazyString(testRuns.map { it.asFile.apply { mkdirs() }.absolutePath }))

    val jacocoAgentJar = jacocoAgentJar.singleFile.absolutePath

    // Set system properties for the test task
    systemProperty("jacocoAgentJar", jacocoAgentJar)
    systemProperty("jacocoDestfile", the<JacocoTaskExtension>().destinationFile!!.absolutePath)

    // Add doLast action for read lock
    doLast {
        val jacocoDestfile = the<JacocoTaskExtension>().destinationFile!!
        FileChannel.open(jacocoDestfile.toPath(), StandardOpenOption.READ).use {
            it.lock(0, Long.MAX_VALUE, true).release()
        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.test {
    the<JacocoTaskExtension>().excludes = listOf("*")
}

val jacocoAnt by configurations.existing
tasks.pluginUnderTestMetadata {
    actions.clear()
    doLast {
        val instrumentedPluginClasspath = temporaryDir.resolve("instrumentedPluginClasspath")
        instrumentedPluginClasspath.deleteRecursively()
        ant.withGroovyBuilder {
            "taskdef"("name" to "instrument",
                "classname" to "org.jacoco.ant.InstrumentTask",
                "classpath" to jacocoAnt.get().asPath)
            "instrument"("destdir" to instrumentedPluginClasspath) {
                pluginClasspath.asFileTree.addToAntBuilder(ant, "resources")
            }
        }

        val properties = Properties();
        if (!pluginClasspath.isEmpty) {
            properties.setProperty(
                IMPLEMENTATION_CLASSPATH_PROP_KEY,
                instrumentedPluginClasspath.absoluteFile.invariantSeparatorsPath
            )
        }
        outputDirectory.file(METADATA_FILE_NAME).get().asFile.outputStream().use {
            properties.store(it, null)
        }
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
    dependsOn(tasks.test)
}

gradlePlugin {
    plugins {
        val pluginId = "my.sample.plugin"
        create(pluginId) {
            id = pluginId
            implementationClass = "my.sample.MySamplePlugin"
            version = "1.0"
            group = "build.logic"
        }
    }
}

class LazyString(private val source: Lazy<String>) : Serializable {
    constructor(source: () -> String) : this(lazy(source))
    constructor(source: Provider<String>) : this(source::get)

    override fun toString() = source.value
}

