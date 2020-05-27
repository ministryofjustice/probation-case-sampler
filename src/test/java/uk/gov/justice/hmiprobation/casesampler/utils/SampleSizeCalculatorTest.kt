package uk.gov.justice.hmiprobation.casesampler.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSizeCalculatorTest.TestType.A
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSizeCalculatorTest.TestType.B
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSizeCalculatorTest.TestType.C
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSizeCalculatorTest.TestType.D
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSizeCalculatorTest.TestType.E
import uk.gov.justice.hmiprobation.casesampler.utils.Type.WITH_BUFFER


class SampleSizeCalculatorTest {

    enum class TestType { A, B, C, D, E }

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
                Result(A, SampleSize(55, "37.30").update(WITH_BUFFER, 66)),
                Result(B, SampleSize(17, "11.34").update(WITH_BUFFER, 20)),
                Result(C, SampleSize(40, "26.87").update(WITH_BUFFER, 48)),
                Result(D, SampleSize(16, "10.60").update(WITH_BUFFER, 19)),
                Result(E, SampleSize(21, "13.89").update(WITH_BUFFER, 25))
        ))
    }

    @Disabled("Being fixed as part of DCS-497" )
    @Test
    fun `check rounding we don't get more samples than requested due to rounding `() {

        (0..90).forEach {
            val sizes = calculateSampleSize(it, mapOf(
                    A to listOfSize(60),
                    B to listOfSize(30),
                    C to listOfSize(30)
            ))

            assertThat(sizes.map { it.size.count }.sum()).isEqualTo(it)
        }
    }
}