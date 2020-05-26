package uk.gov.justice.hmiprobation.casesampler.utils

class Counter(val maxPerKey: Int = Integer.MAX_VALUE) {
    val count = mutableMapOf<String, Int>()

    private fun safeAdd(key: String, number: Int): Int {
        val new = size(key) + number
        assert(new <= maxPerKey, { """$key will be larger than $maxPerKey""" })
        return new
    }

    fun inc(key: String) { count[key] = safeAdd(key,1)}

    fun add(key: String, number: Int) { count[key] = safeAdd(key,number)}

    fun size(key: String): Int = count.getOrDefault(key, 0)

    fun canIncrement(key: String) = size(key) < maxPerKey

    fun spareCapacity(key: String) = Math.max(0, maxPerKey - size(key))
}