package uk.gov.justice.hmiprobation.casesampler.utils

import kotlin.math.roundToInt

data class SampleSize<E : Enum<E>>(
        val type: E,
        val numberOfSamples: Int,
        val percentage: String
)

fun <E: Enum<E>> toSampleSize(numberOfSamples: Int, entry: Map.Entry<E, Double>) = SampleSize<E>(
        type = entry.key,
        numberOfSamples = (numberOfSamples * entry.value).roundToInt(),
        percentage = "%.2f".format(entry.value * 100)
)

fun <E : Enum<E>> calculateSampleSize(numberOfSamples: Int, groupedByType: Map<E, List<Any>>): List<SampleSize<E>> {
    val totalCases = groupedByType.values.map { it.size }.sum()
    val bucketProportions = groupedByType.mapValues { (it.value.size.toDouble() / totalCases) }
    return bucketProportions.map { toSampleSize(numberOfSamples, it) }
}