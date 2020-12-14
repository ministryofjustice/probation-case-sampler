package uk.gov.justice.hmiprobation.casesampler.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.Gender.MALE
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType
import uk.gov.justice.hmiprobation.casesampler.utils.Result
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AllocationCalculatorTest {

  val roAllocationAdjuster: RoAllocationAdjuster = mockk(relaxUnitFun = true)

  val calculator = AllocationCalculator(roAllocationAdjuster)

  @BeforeEach
  fun setup() {
    every { roAllocationAdjuster.adjust(any()) } answers {
      it.invocation.args[0] as List<Result<String>>
    }
  }

  @Test
  fun `check copes with empty cases`() {
    assertThat(calculator.calculate(SampleSize(0, 0), listOf())).isEqualTo(listOf<Bucket>())
  }

  @Test
  fun `simple allocation with even split`() {

    val results = calculator.calculate(
      SampleSize(4, 8),
      listOf(
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01BBB", "ro2"),
        case("N01", "N01BBB", "ro2"),
        case("N02", "N02AAA", "ro1"),
        case("N02", "N02AAA", "ro1"),
        case("N02", "N02BBB", "ro2"),
        case("N02", "N02BBB", "ro2")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(4)
    assertThat(data).filteredOn { it.cluster.id == "N01" }.containsExactly(
      AllocationData(
        cluster = Info("N01", SampleSize(2, 4, "50.00")),
        // 50% means within this cluster this is one LDU of two
        ldu = Info("N01AAA", SampleSize(1, 2, "50.00")),
        // 100% means within this LDU this is the only RO
        ro = Info("ro1", SampleSize(1, 2, "100.00"))
      ),
      AllocationData(
        cluster = Info("N01", SampleSize(2, 4, "50.00")),
        ldu = Info("N01BBB", SampleSize(1, 2, "50.00")),
        ro = Info("ro2", SampleSize(1, 2, "100.00"))
      )
    )
    assertThat(data).filteredOn { it.cluster.id == "N02" }.containsExactly(
      AllocationData(
        cluster = Info("N02", SampleSize(2, 4, "50.00")),
        ldu = Info("N02AAA", SampleSize(1, 2, "50.00")),
        ro = Info("ro1", SampleSize(1, 2, "100.00"))
      ),
      AllocationData(
        cluster = Info("N02", SampleSize(2, 4, "50.00")),
        ldu = Info("N02BBB", SampleSize(1, 2, "50.00")),
        ro = Info("ro2", SampleSize(1, 2, "100.00"))
      )
    )
  }

  @Test
  fun `simple allocation sample characteristics`() {

    val results = calculator.calculate(
      SampleSize(4, 8),
      listOf(
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01BBB", "ro2"),
        case("N01", "N01BBB", "ro2"),
        case("N02", "N02AAA", "ro1"),
        case("N02", "N02AAA", "ro1"),
        case("N02", "N02BBB", "ro2"),
        case("N02", "N02BBB", "ro2")
      )
    )

    val cases = results.flatMap { it.getRandomSamples() }

    assertThat(cases).filteredOn { it.cluster == "N01" }.hasSize(2)
    assertThat(cases).filteredOn { it.cluster == "N02" }.hasSize(2)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro1" }.hasSize(2)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro2" }.hasSize(2)
    assertThat(cases).filteredOn { it.ldu == "N01AAA" }.hasSize(1)
    assertThat(cases).filteredOn { it.ldu == "N01BBB" }.hasSize(1)
    assertThat(cases).filteredOn { it.ldu == "N02AAA" }.hasSize(1)
    assertThat(cases).filteredOn { it.ldu == "N02BBB" }.hasSize(1)
  }

  @Test
  fun `proportionality maintained at cluster level`() {

    val results = calculator.calculate(
      SampleSize(4, 6),
      listOf(
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01AAA", "ro1"),

        case("N02", "N02AAA", "ro1"),
        case("N02", "N02AAA", "ro1"),
        case("N02", "N02AAA", "ro1"),
        case("N02", "N02AAA", "ro1")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(2)
    assertThat(data.find { it.cluster.id == "N01" }).extracting { it!!.cluster }.isEqualTo(Info("N01", SampleSize(1, 2, "33.33")))
    assertThat(data.find { it.cluster.id == "N02" }).extracting { it!!.cluster }.isEqualTo(Info("N02", SampleSize(3, 4, "66.67")))

    val cases = results.flatMap { it.getRandomSamples() }
    assertThat(cases).filteredOn { it.cluster == "N01" }.hasSize(1)
    assertThat(cases).filteredOn { it.cluster == "N02" }.hasSize(3)
  }

  @Test
  fun `proportionality maintained at ldu level`() {

    val results = calculator.calculate(
      SampleSize(4, 6),
      listOf(
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01BBB", "ro1"),
        case("N01", "N01BBB", "ro1"),
        case("N01", "N01BBB", "ro1"),
        case("N01", "N01BBB", "ro1")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(2)
    assertThat(data.find { it.ldu.id == "N01AAA" }).extracting { it!!.ldu }.isEqualTo(Info("N01AAA", SampleSize(1, 2, "33.33")))
    assertThat(data.find { it.ldu.id == "N01BBB" }).extracting { it!!.ldu }.isEqualTo(Info("N01BBB", SampleSize(3, 4, "66.67")))

    val cases = results.flatMap { it.getRandomSamples() }
    assertThat(cases).filteredOn { it.ldu == "N01AAA" }.hasSize(1)
    assertThat(cases).filteredOn { it.ldu == "N01BBB" }.hasSize(3)
  }

  @Test
  fun `proportionality maintained at ro level`() {

    val results = calculator.calculate(
      SampleSize(4, 6),
      listOf(
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01AAA", "ro1"),
        case("N01", "N01AAA", "ro2"),
        case("N01", "N01AAA", "ro2"),
        case("N01", "N01AAA", "ro2"),
        case("N01", "N01AAA", "ro2")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(2)
    assertThat(data.find { it.ro.id == "ro1" }).extracting { it!!.ro }.isEqualTo(Info("ro1", SampleSize(1, 2, "33.33")))
    assertThat(data.find { it.ro.id == "ro2" }).extracting { it!!.ro }.isEqualTo(Info("ro2", SampleSize(3, 4, "66.67")))

    val cases = results.flatMap { it.getRandomSamples() }
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro1" }.hasSize(1)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro2" }.hasSize(3)
  }

  @Test
  fun `complex allocation sample characteristics`() {

    val results = calculator.calculate(
      SampleSize(6, 12),
      listOf(
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),

        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),

        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(4)

    val cases = results.flatMap { it.getRandomSamples() }

    assertThat(cases).hasSize(6)

    assertThat(cases).filteredOn { it.cluster == "N01" }.hasSize(4)
    assertThat(cases).filteredOn { it.cluster == "N02" }.hasSize(2)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro1" }.hasSize(1)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro2" }.hasSize(2)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro3" }.hasSize(1)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro4" }.hasSize(2)
    assertThat(cases).filteredOn { it.ldu == "LDU01" }.hasSize(3)
    assertThat(cases).filteredOn { it.ldu == "LDU02" }.hasSize(1)
    assertThat(cases).filteredOn { it.ldu == "LDU03" }.hasSize(2)
  }

  @Test
  fun `complex allocation larger sample characteristics`() {

    val results = calculator.calculate(
      SampleSize(12, 24),
      listOf(
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),

        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),

        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),
        case("N01", "LDU02", "ro3"),

        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4"),
        case("N02", "LDU03", "ro4")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(4)

    val cases = results.flatMap { it.getRandomSamples() }
    assertThat(cases).hasSize(12)

    assertThat(cases).filteredOn { it.cluster == "N01" }.hasSize(9)
    assertThat(cases).filteredOn { it.cluster == "N02" }.hasSize(3)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro1" }.hasSize(4)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro2" }.hasSize(2)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro3" }.hasSize(3)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro4" }.hasSize(3)
    assertThat(cases).filteredOn { it.ldu == "LDU01" }.hasSize(6)
    assertThat(cases).filteredOn { it.ldu == "LDU02" }.hasSize(3)
    assertThat(cases).filteredOn { it.ldu == "LDU03" }.hasSize(3)
  }

  @Test
  fun `allocation takes into account ro max limits`() {

    val calculator = AllocationCalculator(RoAllocationAdjuster(6))

    val results = calculator.calculate(
      SampleSize(12, 24),
      listOf(
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),
        case("N01", "LDU01", "ro1"),

        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro2"),
        case("N01", "LDU01", "ro3"),
        case("N01", "LDU01", "ro3")
      )
    )

    val data = results.map { it.allocationData }
    assertThat(data).hasSize(3)

    val cases = results.flatMap { it.getRandomSamples() }
    assertThat(cases).hasSize(12)

    assertThat(cases).filteredOn { it.cluster == "N01" }.hasSize(12)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro1" }.hasSize(6)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro2" }.hasSize(4)
    assertThat(cases).filteredOn { it.responsibleOfficer == "ro3" }.hasSize(2)
  }

  fun case(cluster: String, ldu: String, ro: String) = Case(
    familyName = "Smith",
    firstName = "Sam",
    dob = "12/02/2020",
    gender = MALE,
    sentenceType = SentenceType.COMMUNITY_SENTENCE,
    crn = "1111",
    pnc = "2222",
    roshClassification = RiskOfSeriousHarmLevel.HIGH,
    startDate = LocalDate.of(2020, 1, 2),
    endDate = LocalDate.of(2020, 2, 3),
    cluster = cluster,
    ldu = ldu,
    team = "N02AFF",
    responsibleOfficer = ro,
    manager = "Jill Jones",
    officer = "Andy Arland"
  )
}
