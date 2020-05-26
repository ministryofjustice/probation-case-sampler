package uk.gov.justice.hmiprobation.casesampler.services

import org.slf4j.LoggerFactory
import uk.gov.justice.hmiprobation.casesampler.utils.Counter
import uk.gov.justice.hmiprobation.casesampler.utils.Result
import uk.gov.justice.hmiprobation.casesampler.utils.Size
import uk.gov.justice.hmiprobation.casesampler.utils.Size.Adjusted
import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize

data class RoSize(val ro: String, val size: Size) {
    constructor(pair: Pair<String, Size>) : this(pair.first, pair.second)

    fun toPair() = Pair(ro, size)
}

/**
 *  Adjusts provided sample sizes so we don't take more than a given number of cases from a specific RO.
 *  It will attempt to take more cases from other ROs to ensure we have a big enough sample for this specific LDU.
 *  It takes additional cases in a round robin style - choosing ROs with the smallest amount of cases first, potentially
 *   revisiting ROs if additional cases are required
 */
class RoAllocationAdjuster(val maxAllowedCasesPerRo: Int = 6) {

    val roCaseCounter = Counter(maxAllowedCasesPerRo)

    fun adjust(sizes: List<Result<String>>): List<RoSize> {

        val reducedSizes = sizes.map { reduceSizesThatExceedCapacity(it.key, it.size) }

        val numberOfRemovedCases = numberOfRemovedCase(reducedSizes)

        log.info("Cases to reallocate: $numberOfRemovedCases, spare capacity: ${spareCapacity(reducedSizes)}")

        return reallocateRemovedCases(reducedSizes, numberOfRemovedCases)
    }

    private fun numberOfRemovedCase(sizes: List<RoSize>) = sizes.sumBy { (_, size) -> if (size is Adjusted) -size.adjustment else 0 }

    private fun spareCapacity(sizes: List<RoSize>) = sizes.sumBy { (ro, _) -> roCaseCounter.spareCapacity(ro) }

    // Adjust sizes for each RO, so none are assigned more than the maxAllowedCasesPerRo
    private fun reduceSizesThatExceedCapacity(ro: String, size: SampleSize): RoSize {
        val globalCount = roCaseCounter.size(ro)
        val additional = size.count
        val adjustment = Math.max(0, globalCount + size.count - maxAllowedCasesPerRo)
        return if (adjustment > 0) {
            roCaseCounter.add(ro, additional - adjustment)
            log.info("Need to adjust RO cases: $ro, globalCount: $globalCount, additional: $additional,  adjustment: -$adjustment, newTotal: ${roCaseCounter.size(ro)}")
            RoSize(ro, Adjusted(-adjustment, additional - adjustment, size.percentage))
        } else {
            val newCount = size.count
            roCaseCounter.add(ro, newCount)
            log.info("Not adjusting RO cases: $ro, globalCount: $globalCount, additional: $additional, newTotal: ${roCaseCounter.size(ro)}")
            RoSize(ro, size)
        }
    }

    /**
     * Add individual cases to RO's which have spare global capacity from smallest to largest until all removed cases are reallocated
     * @return modified sample sizes of each RO in alphabetic order of the RO's name
     * */
    private fun reallocateRemovedCases(roSizes: List<RoSize>, numberOfRemovedCases: Int): List<RoSize> {
        val roToSize = roSizes.map { it.toPair() }.toMap().toMutableMap()
        val infiniteRos = generateSequence { rosWithSpareCapacity(roSizes) }.flatten().iterator()

        for (i in 0 until numberOfRemovedCases) {
            var allocationSuccessful = false
            for (j in 0 until roToSize.keys.size) { // <- make sure we don't retry previously seen ROs
                val ro = infiniteRos.next()

                if (roCaseCounter.canIncrement(ro)) {
                    addCaseToRo(ro, roToSize)
                    allocationSuccessful = true
                    break
                } else {
                    log.info("Not allocating case to Ro: ${ro} as full, size: ${roCaseCounter.size(ro)}")
                }
            }
            if (!allocationSuccessful) {
                log.warn("Couldn't allocate sample as no capacity left")
            }
        }
        return roToSize.toList().map { RoSize(it) }.sortedBy { it.ro }
    }

    private fun addCaseToRo(ro: String, roToSize: MutableMap<String, Size>) {
        log.info("Current size of ${ro} is ${roCaseCounter.size(ro)}, allocating additional case")
        val size = roToSize[ro]!!
        roCaseCounter.inc(ro)
        roToSize[ro] = when (size) {
            is Adjusted -> Adjusted(size.adjustment + 1, size.numberOfSamples + 1, size.preAdjustedPercentage)
            is SampleSize -> Adjusted(1, size.numberOfSamples + 1, size.percentage)
        }
    }

    private fun rosWithSpareCapacity(roToSize: List<RoSize>) = roToSize
            .sortedBy { (_, value) -> value.count }
            .map { (ro, _) -> ro }


    companion object {
        private val log = LoggerFactory.getLogger(RoAllocationAdjuster::class.java)
    }
}
