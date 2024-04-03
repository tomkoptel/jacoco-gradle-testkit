import java.io.Serializable
import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute

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

tasks.jacocoTestReport {
    executionData(tasks.test.map { test -> test.the<JacocoTaskExtension>().destinationFile })
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

