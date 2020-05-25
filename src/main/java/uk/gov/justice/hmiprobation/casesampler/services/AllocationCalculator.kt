package uk.gov.justice.hmiprobation.casesampler.services

import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.utils.Size
import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize
import uk.gov.justice.hmiprobation.casesampler.utils.calculateSampleSize
import kotlin.random.Random

data class Info(val id: String, val size: Size)

data class RoAllocation(val cluster: Info, val ldu: Info, val ro: Info, val cases: List<Case>) {
    fun getRandomSamples(random: Random = Random) = cases.shuffled(random).take(ro.size.count)
}

/**
 *  Calculates the proportions of samples that are required for each RO whilst maintaining proportionality across
 *  - Cluster
 *  - Local Delivery Unit (LDU)
 *  - Responsible Officer (RO) up to maximum of n
 */
class AllocationCalculator(val size: SampleSize, val cases: List<Case>, val roAllocationAdjuster: RoAllocationAdjuster) {

    fun calculateRoAllocations(): List<RoAllocation> {
        val casesByCluster = cases.groupBy { it.cluster }

        val clusterSizes = calculateSampleSize(size.numberOfSamples, casesByCluster)

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
        val roSizes = roAllocationAdjuster.adjust(sampleSizesForRo)

        return roSizes.fold(mutableListOf()) { result, (ro, size) ->
            result.add(RoAllocation(cluster, ldu, Info(ro, size), casesByRo[ro]!!))
            result
        }
    }
}
