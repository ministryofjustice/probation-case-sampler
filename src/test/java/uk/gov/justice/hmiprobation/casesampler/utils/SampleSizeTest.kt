package uk.gov.justice.hmiprobation.casesampler.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.A
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.B
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.C
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.D
import uk.gov.justice.hmiprobation.casesampler.utils.CasesTest.Type.E
import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize


class CasesTest {

    enum class Type { A, B, C, D, E }

    fun listOfSize(i: Int) = (1..i).toList()

    @Test
    fun `Check simple example`() {

        val sizes = calculateSampleSize(10, mapOf(
                A to listOfSize(50),
                B to listOfSize(50)))

        assertThat(sizes).isEqualTo(listOf(
                Result(A, SampleSize(5, "50.00")),
                Result(B, SampleSize(5, "50.00"))
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
                Result(A, SampleSize(3, "33.33")),
                Result(B, SampleSize(3, "33.33")),
                Result(C, SampleSize(3, "33.33"))
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
                Result(A, SampleSize(55, "37.30")),
                Result(B, SampleSize(17, "11.34")),
                Result(C, SampleSize(40, "26.87")),
                Result(D, SampleSize(16, "10.60")),
                Result(E, SampleSize(21, "13.89"))
        ))
    }

    @Test
    fun `example from doc with 20% buffer`() {

        val sizes = calculateSampleSize(148, mapOf(
                A to listOfSize(454),
                B to listOfSize(138),
                C to listOfSize(327),
                D to listOfSize(129),
                E to listOfSize(169)
        ), 20.00)

        assertThat(sizes).isEqualTo(listOf(
                Result(A, SampleSize(55, 66, "37.30")),
                Result(B, SampleSize(17, 20, "11.34")),
                Result(C, SampleSize(40, 48, "26.87")),
                Result(D, SampleSize(16, 19, "10.60")),
                Result(E, SampleSize(21, 25, "13.89"))
        ))
    }
}