package github.nskvortsov

import github.nskvortsov.teamcity.cleanup.GradleCacheCleanerProvider
import github.nskvortsov.teamcity.cleanup.MavenCacheCleanerProvider
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.DirectoryCleanersProviderContext
import jetbrains.buildServer.agent.DirectoryCleanersRegistry
import jetbrains.buildServer.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.*
import kotlin.properties.Delegates

class SimpleCleanerProvidersTest {

    var registryMap: MutableMap<File, Runnable> by Delegates.notNull()
    var context: DirectoryCleanersProviderContext by Delegates.notNull()
    var registry: DirectoryCleanersRegistry by Delegates.notNull()
    var runningBuild: AgentRunningBuild by Delegates.notNull()

    var tempDir: File by Delegates.notNull()
    var oldHome: String? = null

    @BeforeMethod
    fun setUp() {
        tempDir = FileUtil.createTempDirectory("test", "cleanup")
        FileUtil.copyDir(File("src/test/resources/testData"), tempDir);
        oldHome = System.getProperty("user.home")
        System.setProperty("user.home", tempDir.absolutePath)


        registryMap = HashMap<File, Runnable>()
        context = mock(DirectoryCleanersProviderContext::class.java)
        registry = mock(DirectoryCleanersRegistry::class.java)
        runningBuild = mock(AgentRunningBuild::class.java)
        `when`(registry.addCleaner(any(), any(), any())).thenAnswer { registryMap.put(it.arguments[0] as File, it.arguments[2] as Runnable) }
        `when`(context.runningBuild).thenAnswer { runningBuild }
    }

    @AfterMethod
    fun tearDown() {
        System.setProperty("user.home", oldHome)
        FileUtil.delete(tempDir)
    }

    @Test
    fun testMavenProvider() {
        val provider = MavenCacheCleanerProvider()
        val m2repo = File("${System.getProperty("user.home")}/.m2/repository")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(m2repo)
        registryMap.get(m2repo)?.run()
        assertThat(m2repo).doesNotExist()
    }

    @Test
    fun testGradleProvider() {
        val provider = GradleCacheCleanerProvider()
        val gradleCache = File("${System.getProperty("user.home")}/.gradle/caches")
        provider.registerDirectoryCleaners(context, registry)
        assertThat(registryMap).containsKey(gradleCache)
        registryMap.get(gradleCache)?.run()
        assertThat(gradleCache).doesNotExist()
    }
}