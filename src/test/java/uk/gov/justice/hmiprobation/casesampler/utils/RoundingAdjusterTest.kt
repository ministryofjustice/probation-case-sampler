package uk.gov.justice.hmiprobation.casesampler.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.utils.Type.DECREASED_FOR_ROUNDING
import uk.gov.justice.hmiprobation.casesampler.utils.Type.INCREASED_FOR_ROUNDING

class RoundingAdjusterTest {

  @Test
  fun `adjust empty`() {
    val sampleSizes: SampleSizes<String> = listOf()
    assertThat(adjustForRounding(0, sampleSizes)).isEmpty()
  }

  @Test
  fun `adjust empty, asking for samples`() {
    val sampleSizes: SampleSizes<String> = listOf()
    assertThat(adjustForRounding(1, sampleSizes)).isEmpty()
  }

  @Test
  fun `no adjustment needed`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 10, 10))

    assertThat(adjustForRounding(10, sampleSizes)).isEqualTo(sampleSizes)
  }

  @Test
  fun `one sample under (One type)`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 9, 10))
    assertThat(adjustForRounding(10, sampleSizes)).containsExactly(
      Result("k1", SampleSize(9, 10).update(INCREASED_FOR_ROUNDING, 10))
    )
  }

  @Test
  fun `one sample under (Two types, only added to one of them)`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 9, 10), size("k2", 9, 10))
    assertThat(adjustForRounding(19, sampleSizes)).containsExactly(
      Result("k1", SampleSize(9, 10).update(INCREASED_FOR_ROUNDING, 10)),
      Result("k2", SampleSize(9, 10))
    )
  }

  @Test
  fun `two sample under (Three types, only added to two of them)`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 9, 10), size("k2", 9, 10), size("k3", 9, 10))
    assertThat(adjustForRounding(29, sampleSizes)).containsExactly(
      Result("k1", SampleSize(9, 10).update(INCREASED_FOR_ROUNDING, 10)),
      Result("k2", SampleSize(9, 10).update(INCREASED_FOR_ROUNDING, 10)),
      Result("k3", SampleSize(9, 10))
    )
  }

  @Test
  fun `doesn't add to full smallest`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 8, 8), size("k2", 9, 10), size("k3", 9, 10))
    assertThat(adjustForRounding(27, sampleSizes)).containsExactly(
      Result("k1", SampleSize(8, 8)),
      Result("k2", SampleSize(9, 10).update(INCREASED_FOR_ROUNDING, 10)),
      Result("k3", SampleSize(9, 10))
    )
  }

  @Test
  fun `doesn't add if completely full`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 1, 8), size("k2", 1, 10), size("k3", 2, 12))
    assertThat(adjustForRounding(500, sampleSizes)).containsExactly(
      Result("k1", SampleSize(1, 8).update(INCREASED_FOR_ROUNDING, 8)),
      Result("k2", SampleSize(1, 10).update(INCREASED_FOR_ROUNDING, 10)),
      Result("k3", SampleSize(2, 12).update(INCREASED_FOR_ROUNDING, 12))
    )
  }

  @Test
  fun `one sample over (One type)`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 11, 11))
    assertThat(adjustForRounding(10, sampleSizes)).containsExactly(
      Result("k1", SampleSize(11, 11).update(DECREASED_FOR_ROUNDING, 10))
    )
  }

  @Test
  fun `Only modify largest types when more samples than requested`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 6, 6), size("k2", 10, 10), size("k3", 4, 4))
    assertThat(adjustForRounding(18, sampleSizes)).containsExactly(
      Result("k1", SampleSize(6, 6).update(DECREASED_FOR_ROUNDING, 5)),
      Result("k2", SampleSize(10, 10).update(DECREASED_FOR_ROUNDING, 9)),
      Result("k3", SampleSize(4, 4))
    )
  }

  @Test
  fun `0 samples requested`() {
    val sampleSizes: SampleSizes<String> = listOf(size("k1", 6, 6), size("k2", 10, 10), size("k3", 4, 4))
    assertThat(adjustForRounding(0, sampleSizes)).containsExactly(
      Result("k1", SampleSize(6, 6).update(DECREASED_FOR_ROUNDING, 0)),
      Result("k2", SampleSize(10, 10).update(DECREASED_FOR_ROUNDING, 0)),
      Result("k3", SampleSize(4, 4).update(DECREASED_FOR_ROUNDING, 0))
    )
  }

  private fun size(key: String, size: Int, total: Int) = Result(key, SampleSize(size, total))
}
