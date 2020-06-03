package uk.gov.justice.hmiprobation.casesampler.utils

import uk.gov.justice.hmiprobation.casesampler.utils.Type.INITIAL

enum class Type { INITIAL, WITH_BUFFER, INCREASED_FOR_ROUNDING, DECREASED_FOR_ROUNDING, INCREASED_FOR_RO, DECREASED_FOR_RO }

data class PreviousValue(val type: Type, val numberOfSamples: Int)

data class SampleSize(
        val count: Int,
        val total: Int,
        val originalPercentage: String = "",
        val type: Type = INITIAL,
        val previousValues: List<PreviousValue> = listOf()) {

    fun update(newType: Type, newCount: Int): SampleSize = SampleSize(
            type = newType,
            total = total,
            count = newCount,
            originalPercentage = originalPercentage,
            // only bother adding history entry if new type of change
            previousValues = if (newType == type) previousValues else previousValues + PreviousValue(type, count)
    )

    fun previousChange(): Int = if (previousValues.isEmpty()) count else count - previousValues.last().numberOfSamples
}
