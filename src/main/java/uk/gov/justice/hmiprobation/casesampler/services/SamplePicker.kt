package uk.gov.justice.hmiprobation.casesampler.services

import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.Sample
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSizes
import kotlin.random.Random

class SamplePicker(val random: Random = Random) {

    fun pick(sampleSizes: SampleSizes<Stratum>, casesByStratum: Map<Stratum, List<Case>>): List<Sample> {
        val allocationCalculator = AllocationCalculator(RoAllocationAdjuster())

        val samples = sampleSizes.map { (stratum, size) ->
            val allocations = allocationCalculator.calculate(size, casesByStratum[stratum]!!)
            val cases = allocations.flatMap { it.getRandomSamples(random) }
            val allocationData = allocations.map { it.allocationData }
            Sample(stratum, size, allocationData, cases)
        }

        return samples
    }
}