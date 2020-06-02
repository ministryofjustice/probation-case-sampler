package uk.gov.justice.hmiprobation.casesampler.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.Gender
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel
import uk.gov.justice.hmiprobation.casesampler.dto.Sample
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.COMMUNITY_SENTENCE
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.POST_CUSTODY
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.FEMALE
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.MALE_COMMUNITY_LOW
import uk.gov.justice.hmiprobation.casesampler.utils.Result
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
import java.time.LocalDate
import kotlin.random.Random

class SamplePickerTest {

    lateinit var samplePicker: SamplePicker

    @BeforeEach
    fun setup() {
        samplePicker = SamplePicker(Random(1))
    }

    @Test
    fun `Copes with empty samples`() {
        assertThat(samplePicker.pick(listOf(), mapOf())).isEmpty()
    }

    @Test
    fun `simple case`() {
        val sampleSizes = listOf(
                Result(MALE_COMMUNITY_LOW, SampleSize(2, 4, "50.00")),
                Result(FEMALE, SampleSize(4, 8, "50.00")))

        val cases = mapOf(
                MALE_COMMUNITY_LOW to listOf(
                        case(MALE_COMMUNITY_LOW, "01M"),
                        case(MALE_COMMUNITY_LOW, "02M"),
                        case(MALE_COMMUNITY_LOW, "03M"),
                        case(MALE_COMMUNITY_LOW, "04M")
                ),
                FEMALE to listOf(
                        case(FEMALE, "01F"),
                        case(FEMALE, "02F"),
                        case(FEMALE, "03F"),
                        case(FEMALE, "04F"),
                        case(FEMALE, "05F"),
                        case(FEMALE, "06F"),
                        case(FEMALE, "07F"),
                        case(FEMALE, "08F")
                ))

        assertThat(samplePicker.pick(sampleSizes, cases)).containsExactly(
                Sample(MALE_COMMUNITY_LOW,
                        SampleSize(2, 4, "50.00"),
                        listOf(
                                AllocationData(
                                        cluster = Info("N01", SampleSize(2, 4, "100.00")),
                                        ldu = Info("N01LA2", SampleSize(2, 4, "100.00")),
                                        ro = Info("Mick Red", SampleSize(2, 4, "100.00")))),
                        listOf(
                                case(MALE_COMMUNITY_LOW, "03M"),
                                case(MALE_COMMUNITY_LOW, "02M")
                        )),
                Sample(FEMALE,
                        SampleSize(4, 8, "50.00"),
                        listOf(
                                AllocationData(
                                        cluster = Info("N01", SampleSize(4, 8, "100.00")),
                                        ldu = Info("N01LA2", SampleSize(4, 8, "100.00")),
                                        ro = Info("Mick Red", SampleSize(4, 8, "100.00")))),
                        listOf(
                                case(FEMALE, "07F"),
                                case(FEMALE, "01F"),
                                case(FEMALE, "04F"),
                                case(FEMALE, "03F")
                        )))
    }

    fun case(stratum: Stratum, pnc: String) = Case(
            familyName = "Smith",
            firstName = "Sam",
            dob = "12/02/2020",
            gender = if (stratum == FEMALE) Gender.FEMALE else Gender.MALE,
            sentenceType = if (stratum == FEMALE) POST_CUSTODY else COMMUNITY_SENTENCE,
            crn = "1111",
            pnc = pnc,
            roshClassification = RiskOfSeriousHarmLevel.LOW,
            startDate = LocalDate.of(2020, 1, 2),
            endDate = LocalDate.of(2020, 2, 3),
            cluster = "N01",
            ldu = "N01LA2",
            team = "N02AFF",
            responsibleOfficer = "Mick Red",
            manager = "Jill Jones",
            officer = "Andy Arland")
}