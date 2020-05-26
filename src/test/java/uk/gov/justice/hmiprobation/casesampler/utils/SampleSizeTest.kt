package uk.gov.justice.hmiprobation.casesampler.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.A
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.B
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.C
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.D
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.E


class CasesTest {

    enum class Type { A, B, C, D, E }

    fun listOfSize(i: Int) = (1..i).toList()

    @Test
    fun `Check simple example`() {

        val sizes = calculateSampleSize(10, mapOf(
                A to listOfSize(50),
                B to listOfSize(50)))

        assertThat(sizes).isEqualTo(listOf(
                SampleSize(A, 5, "50.00"),
                SampleSize(B, 5, "50.00")
        ))
    }

    @Test
    fun `can end up with less samples than requested`() {

        val sizes = calculateSampleSize(10, mapOf(
                A to listOfSize(50),
                B to listOfSize(50),
                C to listOfSize(50)
        ))

        assertThat(sizes).isEqualTo(listOf(
                SampleSize(A, 3, "33.33"),
                SampleSize(B, 3, "33.33"),
                SampleSize(C, 3, "33.33")
        ))
    }

    @Test
    fun `example from doc`() {

        val sizes = calculateSampleSize(148, mapOf(
                A to listOfSize(454),
                B to listOfSize(138),
                C to listOfSize(327),
                D to listOfSize(129),
                E to listOfSize(169)
        ))

        assertThat(sizes).isEqualTo(listOf(
                SampleSize(A, 55, "37.30"),
                SampleSize(B, 17, "11.34"),
                SampleSize(C, 40, "26.87"),
                SampleSize(D, 16, "10.60"),
                SampleSize(E, 21, "13.89")
        ))
    }
}