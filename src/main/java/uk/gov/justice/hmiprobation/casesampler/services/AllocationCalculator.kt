package uk.gov.justice.hmiprobation.casesampler.services

import org.slf4j.LoggerFactory
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
import uk.gov.justice.hmiprobation.casesampler.utils.calculateSampleSize
import kotlin.random.Random

data class Info(val id: String, val size: SampleSize)

data class AllocationData(val cluster: Info, val ldu: Info, val ro: Info)

data class RoAllocation(val allocationData: AllocationData, val cases: List<Case>) {

    fun getRandomSamples(random: Random = Random) = cases.shuffled(random).take(allocationData.ro.size.count)
}

/**
 *  Calculates the proportions of samples that are required for each RO whilst maintaining proportionality across
 *  - Cluster
 *  - Local Delivery Unit (LDU)
 *  - Responsible Officer (RO) up to maximum of n
 */
class AllocationCalculator(val roAllocationAdjuster: RoAllocationAdjuster) {

    fun calculate(size: SampleSize, cases: List<Case>): List<RoAllocation> {
        val casesByCluster = cases.groupBy { it.cluster }

        val clusterSizes = calculateSampleSize(size.count, casesByCluster)

        return clusterSizes.fold(mutableListOf()) { result, (cluster, size) ->
            result.addAll(toRoAllocation(Info(cluster, size), casesByCluster[cluster]!!))
            result
        }
    }

    fun toRoAllocation(cluster: Info, cases: List<Case>): MutableList<RoAllocation> {
        val casesByLdu = cases.groupBy { it.ldu }

        val lduSizes = calculateSampleSize(cluster.size.count, casesByLdu)

        return lduSizes.fold(mutableListOf()) { result, (ldu, size) ->
            result.addAll(toRoAllocation(cluster, Info(ldu, size), casesByLdu[ldu]!!))
            result
        }
    }

    fun toRoAllocation(cluster: Info, ldu: Info, cases: List<Case>): Collection<RoAllocation> {
        val casesByRo = cases.groupBy { it.responsibleOfficer }

        val sampleSizesForRo = calculateSampleSize(ldu.size.count, casesByRo)

        log.info("Adjusting sample sizes for ROs in LDU: ${ldu.id}")
        val roSizes = roAllocationAdjuster.adjust(sampleSizesForRo)

        return roSizes.fold(mutableListOf()) { result, (ro, size) ->
            result.add(RoAllocation(AllocationData(cluster, ldu, Info(ro, size)), casesByRo[ro]!!))
            result
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RoAllocationAdjuster::class.java)
    }
}
