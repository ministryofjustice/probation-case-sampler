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

    val counter = Counter(maxAllowedCasesPerRo)

    fun adjust(sizes: List<Result<String>>): List<RoSize> {

        val reducedSizes = sizes.map { toReducedSize(it.key, it.size) }
        val numberOfRemovedCases = numberOfRemovedCase(reducedSizes)
        val (nonFull, full) = splitByCapacity(reducedSizes)
        val spareCapacity = calculateSpareCapacity(reducedSizes) // <-  this doesn't take into consideration global capacity

        log.info("Cases to reallocated: $numberOfRemovedCases, spare capacity: $spareCapacity")

        val reAssigned = assignRemovedCases(nonFull, numberOfRemovedCases)

        return (reAssigned + full).sortedBy { it.ro }
    }

    private fun numberOfRemovedCase(sizes: List<RoSize>) = sizes.sumBy { (_, size) -> if (size is Adjusted) -size.adjustment else 0 }

    private fun splitByCapacity(sizes: List<RoSize>) = sizes.partition { (ro, _) -> counter.size(ro) < maxAllowedCasesPerRo }

    private fun calculateSpareCapacity(sizes: List<RoSize>) = sizes.sumBy { (ro, _) -> Math.max(0, maxAllowedCasesPerRo - counter.size(ro)) }

    private fun assignRemovedCases(roSizes: List<RoSize>, numberOfRemovedCases: Int): List<RoSize> {
        val roToSize = roSizes.map { it.toPair() }.toMap().toMutableMap()
        val infiniteRos = generateSequence { smallestRoToLargest(roSizes) }.flatten().iterator()

        for (i in 0 until numberOfRemovedCases) {
            var found = false
            for (j in 0 until roToSize.keys.size) { // <- make sure we don't retry seen ROs
                val ro = infiniteRos.next()

                if (counter.size(ro) + 1 <= maxAllowedCasesPerRo) {
                    log.info("Allocating case to Ro: ${ro}, before size: ${counter.size(ro)}")
                    val size = roToSize[ro]!!
                    counter.inc(ro)
                    roToSize[ro] = when (size) {
                        is Adjusted -> Adjusted(size.adjustment + 1, size.numberOfSamples + 1, size.preAdjustedPercentage)
                        is SampleSize -> Adjusted(1, size.numberOfSamples + 1, size.percentage)
                    }
                    found = true
                    break
                } else {
                    log.info("Not allocating case to Ro: ${ro} as full, size: ${counter.size(ro)}")
                }
            }
            if (!found) {
                log.warn("Couldn't allocate sample")
            }
        }
        return roToSize.toList().map { RoSize(it) }
    }

    private fun smallestRoToLargest(roToSize: List<RoSize>) = roToSize.sortedBy { (_, value) -> value.count }.map { (ro, _) -> ro }

    // Adjust sizes for each RO, so none are assigned more than the maxAllowedCasesPerRo
    private fun toReducedSize(ro: String, size: SampleSize): RoSize {
        val globalCount = counter.size(ro)
        val more = size.count
        val adjustment = Math.max(0, globalCount + size.count - maxAllowedCasesPerRo)
        return if (adjustment > 0) {
            counter.add(ro,  more - adjustment)
            log.info("RO: $ro, globalCount: $globalCount, additional: $more,  adjustment: -$adjustment, newTotal: ${counter.size(ro)}")
            RoSize(ro, Adjusted(-adjustment, more - adjustment, size.percentage))
        } else {
            val newCount = size.count
            counter.add(ro, newCount)
            log.info("RO: $ro, globalCount: $globalCount, additional: $more, newTotal: ${counter.size(ro)}")
            RoSize(ro, size)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AllocationCalculator::class.java)
    }
}
