package uk.gov.justice.hmiprobation.casesampler.utils

import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize
import kotlin.math.roundToInt


sealed class Size(val count: Int) {

    data class SampleSize(
            val numberOfSamplesWithoutBuffer: Int,
            val numberOfSamples: Int,
            val percentage: String
    ) : Size(numberOfSamples) {
        constructor(numberOfSamples: Int, percentage: String) : this(numberOfSamples, numberOfSamples, percentage)
    }

    data class Adjusted(
            val adjustment: Int,
            val numberOfSamples: Int,
            val preAdjustedPercentage: String
    ) : Size(numberOfSamples)
}

data class Result<K>(val key: K, val size: SampleSize)

fun <K> calculateSampleSize(
        numberOfSamples: Int,
        groupedByType: Map<K, List<Any>>,
        bufferPercentage: Double = 0.0): List<Result<K>> =
        calculateProportions(groupedByType).map { (type, proportion) ->
            Result(type, toSampleSize(numberOfSamples, bufferPercentage, proportion))
        }


fun <K> calculateProportions(groupedByType: Map<K, List<Any>>): Map<K, Double> {
    val totalCases = groupedByType.values.map { it.size }.sum()
    return groupedByType.mapValues { (it.value.size.toDouble() / totalCases) }
}

fun toSampleSize(numberOfSamples: Int, bufferPercentage: Double, proportion: Double): SampleSize {
    val base = (numberOfSamples * proportion).roundToInt()
    val buffer = ((base.toDouble() / 100) * bufferPercentage).roundToInt()
    return SampleSize(
            numberOfSamplesWithoutBuffer = base,
            numberOfSamples = base + buffer,
            percentage = "%.2f".format(proportion * 100)
    )
}