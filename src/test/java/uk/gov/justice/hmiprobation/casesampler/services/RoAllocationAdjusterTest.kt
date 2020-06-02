package uk.gov.justice.hmiprobation.casesampler.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.Result
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
import uk.gov.justice.hmiprobation.casesampler.utils.Type.DECREASED_FOR_RO
import uk.gov.justice.hmiprobation.casesampler.utils.Type.INCREASED_FOR_RO

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
        assertThat(adjuster.adjust(listOf(sampleSize("ro1", 2, 2)))).containsExactly(unmodified("ro1", 2, 2))

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(2)
    }

    @Test
    fun `Copes with multiple results for multiple ROs but no reallocation`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1, 1),
                sampleSize("ro2", 5, 5),
                sampleSize("ro3", 3, 3)
        ))).containsExactly(
                unmodified("ro1", 1, 1),
                unmodified("ro2", 5, 5),
                unmodified("ro3", 3, 3)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(1)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
    }

    @Test
    fun `tracks global state across adjustments`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1, 1),
                sampleSize("ro2", 3, 1),
                sampleSize("ro3", 1, 1)
        ))).containsExactly(
                unmodified("ro1", 1, 1),
                unmodified("ro2", 3, 1),
                unmodified("ro3", 1, 1)
        )

        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1, 1),
                sampleSize("ro2", 2, 2),
                sampleSize("ro3", 2, 2)
        ))).containsExactly(
                unmodified("ro1", 1, 1),
                unmodified("ro2", 2, 2),
                unmodified("ro3", 2, 2)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(2)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(5)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
    }

    @Test
    fun `simple reallocation`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 1, 2),
                sampleSize("ro2", 7, 7)
        ))).containsExactly(
                adjusted("ro1", 2, 2, +1),
                adjusted("ro2", 6, 7, -1)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(2)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
    }

    @Test
    fun `reallocation assigns to smallest`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4, 4),
                sampleSize("ro2", 7, 7),
                sampleSize("ro3", 5, 5),
                sampleSize("ro4", 1, 3),
                sampleSize("ro5", 6, 6)
        ))).containsExactly(
                unmodified("ro1", 4, 4),
                adjusted("ro2", 6, 7, -1),
                unmodified("ro3", 5, 5),
                adjusted("ro4", 2, 3, +1),
                unmodified("ro5", 6, 6)
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
                sampleSize("ro1", 4, 5),
                sampleSize("ro2", 8, 8),
                sampleSize("ro3", 5, 5),
                sampleSize("ro4", 1, 2),
                sampleSize("ro5", 6, 6)
        ))).containsExactly(
                adjusted("ro1", 5, 5, +1),
                adjusted("ro2", 6, 8, -2),
                unmodified("ro3", 5, 5),
                adjusted("ro4", 2, 2, +1),
                unmodified("ro5", 6, 6)
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
                sampleSize("ro1", 4, 6),
                sampleSize("ro2", 12, 12),
                sampleSize("ro3", 5, 6),
                sampleSize("ro4", 1, 5),
                sampleSize("ro5", 6, 6)
        ))).containsExactly(
                adjusted("ro1", 6, 6, +2),
                adjusted("ro2", 6, 12, -6),
                adjusted("ro3", 6, 6, +1),
                adjusted("ro4", 4, 5, +3),
                unmodified("ro5", 6, 6)
        )
    }

    @Test
    fun `re-allocating to full capacity`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4, 8),
                sampleSize("ro2", 12, 12),
                sampleSize("ro3", 5, 8),
                sampleSize("ro4", 1, 8),
                sampleSize("ro5", 8, 8)
        ))).containsExactly(
                adjusted("ro1", 6, 8, +2),
                adjusted("ro2", 6, 12, -6),
                adjusted("ro3", 6, 8, +1),
                adjusted("ro4", 6, 8, +5),
                adjusted("ro5", 6, 8, -2)
        )
    }

    @Test
    fun `re-allocating to beyond capacity drops cases`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4, 8),
                sampleSize("ro2", 120, 120),
                sampleSize("ro3", 5, 8),
                sampleSize("ro4", 1, 8),
                sampleSize("ro5", 8, 8)
        ))).containsExactly(
                adjusted("ro1", 6, 8, +2),
                adjusted("ro2", 6, 120, -114),
                adjusted("ro3", 6, 8, +1),
                adjusted("ro4", 6, 8, +5),
                adjusted("ro5", 6, 8, -2)
        )
    }

    @Test
    fun `re-allocating across adjustments`() {
        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 2, 8),
                sampleSize("ro2", 7, 8),
                sampleSize("ro3", 3, 8)
        ))).containsExactly(
                adjusted("ro1", 3, 8, +1),
                adjusted("ro2", 6, 8, -1),
                unmodified("ro3", 3, 8)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(3)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)

        assertThat(adjuster.adjust(listOf(
                sampleSize("ro1", 4, 8),
                sampleSize("ro2", 1, 8),
                sampleSize("ro4", 2, 8)
        ))).containsExactly(
                adjusted("ro1", 3, 8, -1),
                adjusted("ro2", 0, 8, -1),
                adjusted("ro4", 4, 8, +2)
        )

        assertThat(adjuster.roCaseCounter.size("ro1")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro2")).isEqualTo(6)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
        assertThat(adjuster.roCaseCounter.size("ro3")).isEqualTo(3)
        assertThat(adjuster.roCaseCounter.size("ro4")).isEqualTo(4)
    }

    fun sampleSize(ro: String, samples: Int, max: Int) = Result(ro, SampleSize(samples, max))

    fun adjusted(ro: String, samples: Int, max: Int, adjustment: Int): RoSize {
        val type = if (adjustment > 0) INCREASED_FOR_RO else DECREASED_FOR_RO
        return RoSize(ro, SampleSize(samples - adjustment, max).update(type, samples))
    }

    fun unmodified(ro: String, samples: Int, max: Int) = RoSize(ro, SampleSize(samples, max))

}