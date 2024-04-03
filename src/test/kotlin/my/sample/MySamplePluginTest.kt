package my.sample

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MySamplePluginTest {
    @Rule
    @JvmField
    val testProjectDir = object : TemporaryFolder(System.getProperty("testEnv.workDir").let(::File)) {
        override fun after() = Unit
    }

    private val jacocoAgentJar: String get() = System.getProperty("jacocoAgentJar")!!
    private val jacocoDestfile: String get() = System.getProperty("jacocoDestfile")!!

    @Test
    fun `task is executed`() {
        testProjectDir.newFile("settings.gradle.kts").writeText("""
            import java.lang.management.ManagementFactory
            import javax.management.ObjectName

            abstract class JacocoDumper : BuildService<BuildServiceParameters.None>, AutoCloseable {
                override fun close() {
                    val mBeanServer = ManagementFactory.getPlatformMBeanServer()
                    val jacocoObjectName = ObjectName.getInstance("org.jacoco:type=Runtime")
                    if (mBeanServer.isRegistered(jacocoObjectName)) {
                        mBeanServer.invoke(jacocoObjectName, "dump", arrayOf(true), arrayOf("boolean"))
                    }
                }
            }
            val jacocoDumper = gradle.sharedServices.registerIfAbsent("jacocoDumper", JacocoDumper::class) {}
            jacocoDumper.get()
            gradle.allprojects {
                tasks.configureEach {
                    usesService(jacocoDumper)
                }
            }
        """.trimIndent())
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            plugins {
                id("my.sample.plugin")
            }
        """.trimIndent()
        )
        testProjectDir.newFile("gradle.properties").writeText(
            """
                org.gradle.jvmargs="-javaagent:\$jacocoAgentJar=destfile=\$jacocoDestfile,append=true,dumponexit=false,jmx=true"
            """.trimIndent()
        )
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.root)
            .withArguments("mySampleTask")
            .build()
        val output = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val outputContents = testProjectDir.root.resolve("build/mySampleTaskOutput.txt").readText()
        assert(outputContents.contains(output))
    }
}