package uk.gov.justice.hmiprobation.casesampler.utils

import uk.gov.justice.hmiprobation.casesampler.utils.Type.*
import kotlin.math.roundToInt

data class Result<K>(val key: K, val size: SampleSize)
typealias SampleSizes<K> = List<Result<K>>

fun <K> calculateSampleSize(
        numberOfSamples: Int,
        groupedByType: Map<K, List<Any>>,
        bufferPercentage: Double = 0.0): SampleSizes<K> {
    val proportions = calculateProportions(groupedByType)
    val results = proportions.map { (type, proportion) ->
        Result(type, toSampleSize(numberOfSamples, bufferPercentage, proportion))
    }
    return results
}

fun <K> calculateProportions(groupedByType: Map<K, List<Any>>): Map<K, Double> {
    val totalCases = groupedByType.values.map { it.size }.sum()
    return groupedByType.mapValues { (it.value.size.toDouble() / totalCases) }
}

fun toSampleSize(numberOfSamples: Int, bufferPercentage: Double, proportion: Double): SampleSize {
    val base = (numberOfSamples * proportion).roundToInt()
    val buffer = ((base.toDouble() / 100) * bufferPercentage).roundToInt()
    val originalPercentage = "%.2f".format(proportion * 100)

    return if (buffer > 0) {
        SampleSize(base, originalPercentage).update(WITH_BUFFER, base + buffer)
    } else {
        SampleSize(base, originalPercentage)
    }
}