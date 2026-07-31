package dev.jwarmothiii.clientduedatetracker.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ArchitectureRulesTest {
    private val sourceRoot: Path =
        listOf(Path.of("src/main/java"), Path.of("app/src/main/java"))
            .first { Files.exists(it) }

    @Test
    fun domainsDoNotImportOtherDomainDataOrUiPackages() {
        kotlinSources()
            .filter { it.toString().replace('\\', '/').contains("/domain/") }
            .forEach { source ->
                val normalized = source.toString().replace('\\', '/')
                val owner = normalized.substringAfter("/domain/").substringBefore('/')
                val text = Files.readString(source)
                val forbidden =
                    """import dev\.jwarmothiii\.clientduedatetracker\.domain\.([^.]+)\.(data|ui)\."""
                        .toRegex()
                        .findAll(text)
                        .filter { it.groupValues[1] != owner }
                        .toList()
                assertTrue("$source crosses a domain data/UI boundary: $forbidden", forbidden.isEmpty())
            }
    }

    @Test
    fun businessModelsAreFrameworkFree() {
        kotlinSources()
            .filter { it.toString().replace('\\', '/').contains("/domain/") }
            .filter { it.toString().replace('\\', '/').contains("/model/") }
            .forEach { source ->
                val text = Files.readString(source)
                assertFalse("$source imports Android.", text.contains("import android."))
                assertFalse("$source imports AndroidX.", text.contains("import androidx."))
            }
    }

    @Test
    fun noGenericSharedOrCorePackageExists() {
        val packagePaths =
            kotlinSources().map { it.toString().replace('\\', '/') }.toList()
        assertTrue(packagePaths.none { it.contains("/shared/") || it.contains("/core/") })
    }

    private fun kotlinSources(): Sequence<Path> =
        Files
            .walk(sourceRoot)
            .use { paths -> paths.filter { it.toString().endsWith(".kt") }.toList().asSequence() }
}
