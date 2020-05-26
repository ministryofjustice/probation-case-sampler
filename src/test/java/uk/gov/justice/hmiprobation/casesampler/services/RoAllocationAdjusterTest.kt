package uk.gov.justice.hmiprobation.casesampler.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.Result
import uk.gov.justice.hmiprobation.casesampler.utils.Size.Adjusted
import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize

class RoAllocationAdjusterTest {

    lateinit var adjuster: RoAllocationAdjuster

    @BeforeEach
    fun setup() {
        adjuster = RoAllocationAdjuster(6)
    }

    @Test
    fun `Copes with empty list`() {
        assertThat(adjuster.adjust(listOf())).isEmpty()
    }

    @Test
    fun `Copes with single result, less than max`() {
        assertThat(adjuster.adjust(listOf(sampleSize("ro1", 2)))).containsExactly(size("ro1", 2))

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(2)
    }

    @Test
    fun `Copes with multiple results for multiple ROs but no reallocation`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1),
                sampleSize("ro2", 5),
                sampleSize("ro3", 3)
        ))).containsExactly(
                size("ro1", 1),
                size("ro2", 5),
                size("ro3", 3)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(1)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
    }

    @Test
    fun `tracks global state across adjustments`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1),
                sampleSize("ro2", 3),
                sampleSize("ro3", 1)
        ))).containsExactly(
                size("ro1", 1),
                size("ro2", 3),
                size("ro3", 1)
        )

        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1),
                sampleSize("ro2", 2),
                sampleSize("ro3", 2)
        ))).containsExactly(
                size("ro1", 1),
                size("ro2", 2),
                size("ro3", 2)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(2)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
    }

    @Test
    fun `simple reallocation`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1),
                sampleSize("ro2", 7)
        ))).containsExactly(
                adjusted("ro1", 2, +1),
                adjusted("ro2", 6, -1)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(2)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
    }

    @Test
    fun `reallocation assigns to smallest`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4),
                sampleSize("ro2", 7),
                sampleSize("ro3", 5),
                sampleSize("ro4", 1),
                sampleSize("ro5", 6)
        ))).containsExactly(
                size("ro1", 4),
                adjusted("ro2", 6, -1),
                size("ro3", 5),
                adjusted("ro4", 2, +1),
                size("ro5", 6)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(4)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro4")).isEqualTo(2)
        assertThat(adjuster.roCaseCounter.size("ro5")).isEqualTo(6)
    }

    @Test
    fun `re-allocating two values`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4),
                sampleSize("ro2", 8),
                sampleSize("ro3", 5),
                sampleSize("ro4", 1),
                sampleSize("ro5", 6)
        ))).containsExactly(
                adjusted("ro1", 5, +1),
                adjusted("ro2", 6, -2),
                size("ro3", 5),
                adjusted("ro4", 2, +1),
                size("ro5", 6)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro4")).isEqualTo(2)
        assertThat(adjuster.roCaseCounter.size("ro5")).isEqualTo(6)
    }

    @Test
    fun `re-allocating multiple values`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4),
                sampleSize("ro2", 12),
                sampleSize("ro3", 5),
                sampleSize("ro4", 1),
                sampleSize("ro5", 6)
        ))).containsExactly(
                adjusted("ro1", 6, +2),
                adjusted("ro2", 6, -6),
                adjusted("ro3", 6, +1),
                adjusted("ro4", 4, +3),
                size("ro5", 6)
        )
    }

    @Test
    fun `re-allocating to full capacity`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4),
                sampleSize("ro2", 12),
                sampleSize("ro3", 5),
                sampleSize("ro4", 1),
                sampleSize("ro5", 8)
        ))).containsExactly(
                adjusted("ro1", 6, +2),
                adjusted("ro2", 6, -6),
                adjusted("ro3", 6, +1),
                adjusted("ro4", 6, +5),
                adjusted("ro5", 6, -2)
        )
    }

    @Test
    fun `re-allocating to beyond capacity drops cases`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4),
                sampleSize("ro2", 120),
                sampleSize("ro3", 5),
                sampleSize("ro4", 1),
                sampleSize("ro5", 8)
        ))).containsExactly(
                adjusted("ro1", 6, +2),
                adjusted("ro2", 6, -114),
                adjusted("ro3", 6, +1),
                adjusted("ro4", 6, +5),
                adjusted("ro5", 6, -2)
        )
    }

    @Test
    fun `re-allocating across adjustments`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 2),
                sampleSize("ro2", 7),
                sampleSize("ro3", 3)
        ))).containsExactly(
                adjusted("ro1", 3, +1),
                adjusted("ro2", 6, -1),
                size("ro3", 3)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(3)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)

        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4),
                sampleSize("ro2", 1),
                sampleSize("ro4", 2)
        ))).containsExactly(
                adjusted("ro1", 3, -1),
                adjusted("ro2", 0, -1),
                adjusted("ro4", 4, +2)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
        assertThat(adjuster.roCaseCounter.size("ro4")).isEqualTo(4)
    }

    fun sampleSize(ro: String, samples: Int) = Result(ro, SampleSize(samples, "not relevant"))
    fun size(ro: String, samples: Int) = RoSize(ro, SampleSize(samples, "not relevant"))
    fun adjusted(ro: String, samples: Int, adjustment: Int) = RoSize(ro, Adjusted(adjustment, samples, "not relevant"))

}