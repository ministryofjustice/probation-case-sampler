package uk.gov.justice.hmiprobation.casesampler.dto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.hmiprobation.casesampler.dto.Gender.FEMALE
import uk.gov.justice.hmiprobation.casesampler.dto.Gender.MALE
import uk.gov.justice.hmiprobation.casesampler.dto.Gender.OTHER
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel.HIGH
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel.LOW
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.COMMUNITY_SENTENCE
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.POST_CUSTODY
import java.time.LocalDate


class CasesTest {

    @Test
    fun `Check redaction` () {
        assertThat(case().copy(firstName = "Jim", familyName = "Jens").isRedacted()).isFalse()
        assertThat(case().copy(firstName = "*", familyName = "Jens").isRedacted()).isTrue()
        assertThat(case().copy(firstName = "Jim", familyName = "*").isRedacted()).isTrue()
        assertThat(case().copy(firstName = "", familyName = "").isRedacted()).isFalse()
    }

    @Test
    fun `Check later than commencement date` () {
        assertThat(case().copy(startDate = LocalDate.now().minusDays(1)).isLaterCommencementDate()).isFalse()
        assertThat(case().copy(startDate = LocalDate.now()).isLaterCommencementDate()).isFalse()
        assertThat(case().copy(startDate = LocalDate.now().plusDays(1)).isLaterCommencementDate()).isTrue()
    }

    @Test
    fun `Check is same offender` () {
        val case = case()
        val differentValue = "someDifferentValue"

        assertThat(case().copy().isSameOffender(case)).isTrue()

        assertThat(case().copy(pnc = case.pnc, firstName = differentValue).isSameOffender(case)).isTrue()
        assertThat(case().copy(pnc = case.pnc, familyName = differentValue).isSameOffender(case)).isTrue()
        assertThat(case().copy(pnc = case.pnc, dob = differentValue).isSameOffender(case)).isTrue()

        assertThat(case().copy(pnc = differentValue).isSameOffender(case)).isTrue()
        assertThat(case().copy(pnc = differentValue, firstName = differentValue).isSameOffender(case)).isFalse()
        assertThat(case().copy(pnc = differentValue, familyName = differentValue).isSameOffender(case)).isFalse()
        assertThat(case().copy(pnc = differentValue, dob = differentValue).isSameOffender(case)).isFalse()
    }

    @Nested
    @DisplayName("isEarliestCaseForOffender")
    inner class IsEarliestCaseForOffender {

        @Test
        fun `is earliest if no other cases`() {
            val case = case()
            assertThat(case.isEarliestCaseForOffender(listOf(case))).isTrue()
        }

        @Test
        fun `is earliest if only later cases`() {
            val case = case()
            val laterCase = case().copy(startDate = case.startDate.plusDays(1))
            assertThat(case.isEarliestCaseForOffender(listOf(case, laterCase))).isTrue()
        }

        @Test
        fun `is not earliest if earlier cases`() {
            val case = case()
            val earlierCase = case().copy(startDate = case.startDate.minusDays(1))
            assertThat(case.isEarliestCaseForOffender(listOf(case, earlierCase))).isFalse()
        }

        @Test
        fun `Only earliest if first and match and same equality value`() {
            val case = case().copy(responsibleOfficer = "aa")
            val sameCaseButDifferent = case().copy(responsibleOfficer = "bb")
            assertThat(case.isEarliestCaseForOffender(listOf(case, sameCaseButDifferent))).isTrue()
            assertThat(case.isEarliestCaseForOffender(listOf(sameCaseButDifferent, case))).isFalse()
        }

        @Test
        fun `Only earliest if first, match and equality match and same instance`() {
            val case = case()
            val sameCaseButDifferent = case()
            assertThat(case.isEarliestCaseForOffender(listOf(case, sameCaseButDifferent))).isTrue()
            assertThat(case.isEarliestCaseForOffender(listOf(sameCaseButDifferent, case))).isFalse()
        }
    }

    @Nested
    @DisplayName("isExcluded")
    inner class IsExcluded {
        @Test
        fun `redacted is excluded`() {
            assertThat(case().copy(firstName = "*", familyName = "Jens").isExcluded).isTrue()
        }
        @Test
        fun `other gender is excluded`() {
            assertThat(case().copy(gender = Gender.OTHER).isExcluded).isTrue()
        }
        @Test
        fun `cases that start after now are excluded`() {
            assertThat(case().copy(startDate = LocalDate.now().plusDays(1)).isExcluded).isTrue()
        }
    }

    @Nested
    @DisplayName("get type")
    inner class GetType {
        @Test
        fun `Check get female Type`() {

            assertThat(case()
                    .copy(gender = FEMALE, sentenceType = COMMUNITY_SENTENCE, roshClassification = LOW).getStratum())
                    .isEqualTo(Stratum.FEMALE)

            assertThat(case()
                    .copy(gender = FEMALE, sentenceType = POST_CUSTODY, roshClassification = LOW).getStratum())
                    .isEqualTo(Stratum.FEMALE)

            assertThat(case()
                    .copy(gender = FEMALE, sentenceType = COMMUNITY_SENTENCE, roshClassification = HIGH).getStratum())
                    .isEqualTo(Stratum.FEMALE)

            assertThat(case()
                    .copy(gender = FEMALE, sentenceType = POST_CUSTODY, roshClassification = HIGH).getStratum())
                    .isEqualTo(Stratum.FEMALE)
        }

        @Test
        fun `Check get Male Community Low`() {

            assertThat(case()
                    .copy(gender = MALE, sentenceType = COMMUNITY_SENTENCE, roshClassification = LOW).getStratum())
                    .isEqualTo(Stratum.MALE_COMMUNITY_LOW)
        }

        @Test
        fun `Check get Male Community Non Low`() {

            assertThat(case()
                    .copy(gender = MALE, sentenceType = COMMUNITY_SENTENCE, roshClassification = HIGH).getStratum())
                    .isEqualTo(Stratum.MALE_COMMUNITY_NON_LOW)
        }

        @Test
        fun `Check get Male Post Custody Low`() {

            assertThat(case()
                    .copy(gender = MALE, sentenceType = POST_CUSTODY, roshClassification = LOW).getStratum())
                    .isEqualTo(Stratum.MALE_POST_CUSTODY_LOW)
        }

        @Test
        fun `Check get Male Post Custody Non Low`() {

            assertThat(case()
                    .copy(gender = MALE, sentenceType = POST_CUSTODY, roshClassification = HIGH).getStratum())
                    .isEqualTo(Stratum.MALE_POST_CUSTODY_NON_LOW)
        }

        @Test
        fun `Check other gender not handled`() {

            assertThatThrownBy {
                (case()
                        .copy(gender = OTHER, sentenceType = POST_CUSTODY, roshClassification = HIGH).getStratum())
            }.hasMessage("Cannot determine type of stratification")
        }
    }

    fun case() = Case(
            familyName = "Smith",
            firstName = "Sam",
            dob = "12/02/2020",
            gender = MALE,
             sentenceType = COMMUNITY_SENTENCE,
            crn = "1111",
            pnc = "2222",
            roshClassification = HIGH,
            startDate = LocalDate.of(2020, 1, 2),
            endDate = LocalDate.of(2020, 2, 3),
            cluster = "N01",
            ldu = "N01LA2",
            team = "N02AFF",
            responsibleOfficer = "Mick Red",
            manager = "Jill Jones",
            officer = "Andy Arland")
}