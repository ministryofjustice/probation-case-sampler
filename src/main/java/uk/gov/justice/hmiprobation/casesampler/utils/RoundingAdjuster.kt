package uk.gov.justice.hmiprobation.casesampler.utils

import uk.gov.justice.hmiprobation.casesampler.utils.Type.DECREASED_FOR_ROUNDING
import uk.gov.justice.hmiprobation.casesampler.utils.Type.INCREASED_FOR_ROUNDING

fun <K> adjustForRounding(numberOfSamples: Int, results: SampleSizes<K>): SampleSizes<K> {
    val totalSampleSize = totalSize(results)
    return when {
        results.isEmpty() -> results
        totalSampleSize < numberOfSamples -> addAdditionalSamples(numberOfSamples - totalSampleSize, results)
        totalSampleSize > numberOfSamples -> removeAdditionalSamples(totalSampleSize - numberOfSamples, results)
        else -> results
    }
}

private fun <K> totalSize(results: SampleSizes<K>): Int = results.sumBy { (_, size) -> size.count }

private fun <K> removeAdditionalSamples(samplesToRemove: Int, results: SampleSizes<K>): SampleSizes<K> {
    val keyToSize = results.toMap().toMutableMap()
    val infiniteKeys = infiniteKeysIterator(results.sortedByDescending { (_, size) -> size.count })

    (0 until samplesToRemove).forEach { removeSample(keyToSize, infiniteKeys) }

    return keyToSize.toList()
}

private fun <K> removeSample(keyToSize: MutableMap<K, SampleSize>, infiniteKeys: Iterator<K>) {
    (0 until keyToSize.size).forEach {
        val key = infiniteKeys.next()
        if (keyToSize[key]!!.count > 0) {
            keyToSize.computeIfPresent(key, { _, size -> size.update(DECREASED_FOR_ROUNDING, size.count - 1) })
            return
        }
    }
}

private fun <K> addAdditionalSamples(samplesToAdd: Int, results: List<Result<K>>): List<Result<K>> {
    val keyToSize = results.toMap().toMutableMap()
    val infiniteKeys = infiniteKeysIterator(results.sortedBy { (_, size) -> size.count })
    (0 until samplesToAdd).forEach {
        addSample(keyToSize, infiniteKeys)
    }

    return keyToSize.toList()
}

private fun <K> addSample(keyToSize: MutableMap<K, SampleSize>, infiniteKeys: Iterator<K>) {
    (0 until keyToSize.size).forEach {
        val key = infiniteKeys.next()
        if (keyToSize[key]!!.count < keyToSize[key]!!.total) {
            keyToSize.computeIfPresent(key, { _, size -> size.update(INCREASED_FOR_ROUNDING, size.count + 1) })
            return
        }
    }
}

private fun <K> infiniteKeysIterator(results: List<Result<K>>) = generateSequence {
    results
            .map { (key, _) -> key }
}.flatten().iterator()