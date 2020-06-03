package uk.gov.justice.hmiprobation.casesampler.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.Type.DECREASED_FOR_RO
import uk.gov.justice.hmiprobation.casesampler.utils.Type.INCREASED_FOR_RO
import uk.gov.justice.hmiprobation.casesampler.utils.Type.INITIAL
import uk.gov.justice.hmiprobation.casesampler.utils.Type.WITH_BUFFER


class SampleSizeTest {

    @Test
    fun `check update`() {

        assertThat(
                SampleSize(1, 1, "100%")
                        .update(WITH_BUFFER, 5)
        ).isEqualTo(
                SampleSize(5, 1,"100%", WITH_BUFFER, listOf(
                        PreviousValue(INITIAL, 1)
                ))
        )
    }

    @Test
    fun `check multiple updates`() {

        assertThat(
                SampleSize(1, 1, "100%")
                        .update(WITH_BUFFER, 5)
                        .update(INCREASED_FOR_RO, 4)
                        .update(DECREASED_FOR_RO, 6)
        ).isEqualTo(
                SampleSize(6, 1, "100%", DECREASED_FOR_RO, listOf(
                        PreviousValue(INITIAL, 1),
                        PreviousValue(WITH_BUFFER, 5),
                        PreviousValue(INCREASED_FOR_RO, 4)
                ))
        )

    }

    @Test
    fun `update ignores merges subsequent event types`() {

        assertThat(
                SampleSize(1, 1, "100%")
                        .update(WITH_BUFFER, 5)
                        .update(WITH_BUFFER, 6)
                        .update(INCREASED_FOR_RO, 4)
                        .update(INCREASED_FOR_RO, 2)
                        .update(WITH_BUFFER, 6)
                        .update(DECREASED_FOR_RO, 7)
        ).isEqualTo(
                SampleSize(7, 1, "100%", DECREASED_FOR_RO, listOf(
                        PreviousValue(INITIAL, 1),
                        PreviousValue(WITH_BUFFER, 6),
                        PreviousValue(INCREASED_FOR_RO, 2),
                        PreviousValue(WITH_BUFFER, 6)
                ))
        )
    }

    @Test
    fun `check positive size of change`() {

        assertThat(SampleSize(1, 1, "100%")
                .update(WITH_BUFFER, 5)
                .previousChange()).isEqualTo(4)
    }

    @Test
    fun `check negative size of change`() {

        assertThat(SampleSize(5, 5, "100%")
                .update(WITH_BUFFER, 1)
                .previousChange()).isEqualTo(-4)
    }

    @Test
    fun `check size of change with merged events`() {

        assertThat(SampleSize(1, 1, "100%")
                .update(WITH_BUFFER, 2)
                .update(WITH_BUFFER, 4)
                .update(WITH_BUFFER, 6)
                .previousChange()
        ).isEqualTo(5)
    }

    @Test
    fun `check size of change is always based on last change`() {

        assertThat(SampleSize(1, 1, "100%")
                .update(WITH_BUFFER, 2)
                .update(INCREASED_FOR_RO, 4)
                .update(DECREASED_FOR_RO, 6)
                .previousChange()
        ).isEqualTo(2)
    }
}