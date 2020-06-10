package uk.gov.justice.hmiprobation.casesampler.utils

import uk.gov.justice.hmiprobation.casesampler.utils.Type.WITH_BUFFER
import kotlin.math.roundToInt

typealias Result<K> = Pair<K, SampleSize>
typealias SampleSizes<K> = List<Result<K>>

fun <K> calculateSampleSize(
        numberOfSamples: Int,
        groupedByType: Map<K, List<Any>>,
        bufferPercentage: Double = 0.0): SampleSizes<K> {
    val results = calculateProportions(groupedByType).map { (type, proportion) ->
        Result(type, toSampleSize(numberOfSamples, groupedByType[type]?.size ?: 0, bufferPercentage, proportion))
    }

    val buffer = calculateBuffer(numberOfSamples, bufferPercentage)
    return adjustForRounding(numberOfSamples + buffer, results)
}

fun <K> calculateProportions(groupedByType: Map<K, List<Any>>): Map<K, Double> {
    val totalCases = groupedByType.values.map { it.size }.sum()
    return groupedByType.mapValues { (it.value.size.toDouble() / totalCases) }
}

fun toSampleSize(numberOfSamples: Int, total: Int, bufferPercentage: Double, proportion: Double): SampleSize {
    val base = Math.min(total, (numberOfSamples * proportion).roundToInt())
    val buffer = calculateBuffer(base, bufferPercentage)
    val originalPercentage = "%.2f".format(proportion * 100)
    val adjustedValue = Math.min(total, base + buffer)

    return if (base != adjustedValue) {
        SampleSize(base, total, originalPercentage).update(WITH_BUFFER, adjustedValue)
    } else {
        SampleSize(base, total, originalPercentage)
    }
}

private fun calculateBuffer(numberOfSamples: Int, bufferPercentage: Double) =
        ((numberOfSamples.toDouble() / 100) * bufferPercentage).roundToInt()
