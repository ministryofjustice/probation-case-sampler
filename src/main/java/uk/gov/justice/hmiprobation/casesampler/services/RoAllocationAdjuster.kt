package uk.gov.justice.hmiprobation.casesampler.services

import org.slf4j.LoggerFactory
import uk.gov.justice.hmiprobation.casesampler.utils.Counter
import uk.gov.justice.hmiprobation.casesampler.utils.Result
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
import uk.gov.justice.hmiprobation.casesampler.utils.Type.DECREASED_FOR_RO
import uk.gov.justice.hmiprobation.casesampler.utils.Type.INCREASED_FOR_RO

data class RoSize(val ro: String, val size: SampleSize) {
    constructor(pair: Pair<String, SampleSize>) : this(pair.first, pair.second)

    fun toPair() = ro to size
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

    private fun numberOfRemovedCase(sizes: List<RoSize>) = sizes.sumBy { (_, size) -> if (size.type == DECREASED_FOR_RO) Math.abs(size.previousChange()) else 0 }

    private fun spareCapacity(sizes: List<RoSize>) = sizes.sumBy { (ro, _) -> roCaseCounter.spareCapacity(ro) }

    // Adjust sizes for each RO, so none are assigned more than the maxAllowedCasesPerRo
    private fun reduceSizesThatExceedCapacity(ro: String, size: SampleSize): RoSize {
        val globalCount = roCaseCounter.size(ro)
        val additional = size.count
        val adjustment = Math.max(0, globalCount + size.count - maxAllowedCasesPerRo)
        return if (adjustment > 0) {
            roCaseCounter.add(ro, additional - adjustment)
            log.info("Need to adjust RO cases: $ro, globalCount: $globalCount, additional: $additional,  adjustment: -$adjustment, newTotal: ${roCaseCounter.size(ro)}")
            RoSize(ro, size.update(DECREASED_FOR_RO, additional - adjustment))
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

        val infiniteRos = infiniteRoIterator(roSizes)

        (0 until numberOfRemovedCases).forEach { allocateCase(roToSize, infiniteRos) }

        return roToSize.toList().map { RoSize(it) }.sortedBy { it.ro }
    }

    private fun allocateCase(roToSize: MutableMap<String, SampleSize>, infiniteRos: Iterator<String>) {
        (0 until roToSize.size).forEach { // <- make sure we don't retry previously seen ROs
            val ro = infiniteRos.next()

            if (roCaseCounter.canIncrement(ro)) {
                addCaseToRo(ro, roToSize)
                return
            } else {
                log.info("Not allocating case to Ro: ${ro} as full, size: ${roCaseCounter.size(ro)}")
            }
        }
        log.warn("Couldn't allocate sample as no capacity left")
    }

    private fun addCaseToRo(ro: String, roToSize: MutableMap<String, SampleSize>) {
        log.info("Current size of ${ro} is ${roCaseCounter.size(ro)}, allocating additional case")
        val size = roToSize[ro]!!
        roCaseCounter.inc(ro)
        roToSize[ro] = size.update(INCREASED_FOR_RO, size.count + 1)
    }

    private fun infiniteRoIterator(roToSize: List<RoSize>) = generateSequence {
        roToSize
            .sortedBy { (_, value) -> value.count }
            .map { (ro, _) -> ro } }.flatten().iterator()

    companion object {
        private val log = LoggerFactory.getLogger(RoAllocationAdjuster::class.java)
    }
}
