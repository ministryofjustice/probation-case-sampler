package uk.gov.justice.hmiprobation.casesampler.dto

import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel.LOW
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel.NON_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.COMMUNITY_SENTENCE
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.POST_CUSTODY
import uk.gov.justice.hmiprobation.casesampler.dto.Stratification.FEMALE
import uk.gov.justice.hmiprobation.casesampler.dto.Stratification.MALE_COMMUNITY_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.Stratification.MALE_COMMUNITY_NON_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.Stratification.MALE_POST_CUSTODY_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.Stratification.MALE_POST_CUSTODY_NON_LOW
import java.time.LocalDate

enum class Gender { MALE, FEMALE, OTHER }

enum class RiskOfSeriousHarmLevel { LOW, NON_LOW }

enum class SentenceType {
    //Community Sentence (CO & SSO)
    COMMUNITY_SENTENCE,

    // Post-custody (Licence/PSS)
    POST_CUSTODY
}

enum class Stratification {
    MALE_COMMUNITY_NON_LOW,
    MALE_COMMUNITY_LOW,
    MALE_POST_CUSTODY_NON_LOW,
    MALE_POST_CUSTODY_LOW,
    FEMALE,
}

data class Case(val familyName: String,
                val firstName: String,
                val dob: String,
                val gender: Gender,
                val sentenceType: SentenceType,
                val crn: String,
                val pnc: String,
                //Risk of Serious Harm [RoSH] Classification
                val roshClassification: RiskOfSeriousHarmLevel,
                // Sentence date or release on licence date
                val startDate: LocalDate,
                // Order or licence terminated
                val endDate: LocalDate,
                val cluster: String,
                val ldu: String,
                val team: String,
                val responsibleOfficer: String,
                val manager: String,
                val officer: String) {

    internal fun isRedacted() = firstName.contains('*') || familyName.contains('*')

    internal fun isLaterCommencementDate() = startDate.isAfter(LocalDate.now())

    internal fun isOtherGender() = gender == Gender.OTHER

    fun isExcluded() = isRedacted() || isLaterCommencementDate() || isOtherGender()

    internal fun isSameOffender(other: Case): Boolean = pnc == other.pnc || (firstName == other.firstName && familyName == other.familyName && dob == other.dob)

    // Ensures, that this case is the earliest of any duplicates in the passed in list
    fun isEarliestCaseForOffender(cases: List<Case>): Boolean {
        val earliestCase = cases.asSequence()
                .filter { isSameOffender(it) }
                .sortedBy { it.startDate }
                .first()
        return this === earliestCase
    }

    fun getStratification() = when (gender) {
        Gender.FEMALE -> FEMALE
        Gender.MALE -> when (sentenceType) {
            COMMUNITY_SENTENCE -> when (roshClassification) {
                LOW -> MALE_COMMUNITY_LOW
                NON_LOW -> MALE_COMMUNITY_NON_LOW
            }
            POST_CUSTODY -> when (roshClassification) {
                LOW -> MALE_POST_CUSTODY_LOW
                NON_LOW -> MALE_POST_CUSTODY_NON_LOW
            }
        }
        else -> throw Exception("Cannot determine type of stratification")
    }

}
