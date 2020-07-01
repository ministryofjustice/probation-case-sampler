package uk.gov.justice.hmiprobation.casesampler.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.COMMUNITY_SENTENCE
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType.POST_CUSTODY
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.FEMALE
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.MALE_COMMUNITY_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.MALE_COMMUNITY_NON_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.MALE_POST_CUSTODY_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.MALE_POST_CUSTODY_NON_LOW
import java.time.LocalDate

/**
 * Long List of all cases within
 * - NPS region being inspected
 * - Date range being inspected
 */
typealias PrimaryCaseSample = List<Case>

enum class Gender {
    @JsonProperty("M")
    MALE,

    @JsonProperty("F")
    FEMALE, OTHER
}

enum class RiskOfSeriousHarmLevel(val low: Boolean) {
    @JsonAlias("RLRH")
    LOW(true),

    @JsonAlias("RMRH")
    MEDIUM(false),

    @JsonAlias("RHRH")
    HIGH(false),

    @JsonAlias("RVRH")
    VERY_HIGH(false)
}


enum class SentenceType {
    //Community Sentence (CO & SSO)
    @JsonAlias(value = ["ORA Community Order", "ORA Suspended Sentence Order", "CJA - Youth Rehabilitation Order"])
    COMMUNITY_SENTENCE,

    // Post-custody (Licence/PSS)
    @JsonAlias(value = ["Adult Licence", "ORA Supervision Default Order"])
    POST_CUSTODY
}

enum class Stratum {
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
                @JsonFormat(pattern = "dd/MM/yyyy")
                val startDate: LocalDate,
        // Order or licence terminated
                @JsonFormat(pattern = "dd/MM/yyyy")
                val endDate: LocalDate?,
                val cluster: String,
                val ldu: String,
                val team: String,
                val responsibleOfficer: String,
                val manager: String,
                val officer: String) {

    @JsonIgnore
    internal fun isRedacted() = firstName.contains('*') || familyName.contains('*')

    @JsonIgnore
    internal fun isLaterCommencementDate() = startDate.isAfter(LocalDate.now())

    @JsonIgnore
    internal fun isOtherGender() = gender == Gender.OTHER

    @JsonIgnore
    val isExcluded = isRedacted() || isLaterCommencementDate() || isOtherGender()

    @JsonIgnore
    internal fun isSameOffender(other: Case): Boolean = pnc == other.pnc || (firstName == other.firstName && familyName == other.familyName && dob == other.dob)

    // Ensures, that this case is the earliest of any duplicates in the passed in list
    fun isEarliestCaseForOffender(cases: List<Case>): Boolean {
        val earliestCase = cases.asSequence()
                .filter { isSameOffender(it) }
                .sortedBy { it.startDate }
                .first()
        return this === earliestCase
    }

    fun getStratum() = when (gender) {
        Gender.FEMALE -> FEMALE
        Gender.MALE -> when (sentenceType) {
            COMMUNITY_SENTENCE -> when (roshClassification.low) {
                true -> MALE_COMMUNITY_LOW
                false -> MALE_COMMUNITY_NON_LOW
            }
            POST_CUSTODY -> when (roshClassification.low) {
                true -> MALE_POST_CUSTODY_LOW
                false -> MALE_POST_CUSTODY_NON_LOW
            }
        }
        else -> throw Exception("Cannot determine type of stratification")
    }
}
