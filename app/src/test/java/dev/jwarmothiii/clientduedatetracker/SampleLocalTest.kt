package dev.jwarmothiii.clientduedatetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleLocalTest {
    @Test
    fun sampleLocalTestRunsOnJvm() {
        val dueSoonCount = listOf("assessment", "treatment-plan", "discharge").size

        assertEquals(3, dueSoonCount)
    }
}
