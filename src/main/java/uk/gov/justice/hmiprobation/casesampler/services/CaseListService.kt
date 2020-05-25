package uk.gov.justice.hmiprobation.casesampler.services

import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSample
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisional
import uk.gov.justice.hmiprobation.casesampler.utils.calculateSampleSize


class CaseListService() {

    fun process(sampleSize: Int, bufferPercentage: Double, longlist: PrimaryCaseSample): PrimaryCaseSampleProvisional {

        val casesByStratum = longlist.asSequence()
                .filter { it.isExcluded }
                .filter { it.isEarliestCaseForOffender(longlist) }
                .groupBy { it.getStratum() }

        val sampleSizes = calculateSampleSize(sampleSize, casesByStratum, bufferPercentage)

//        val samples = sampleSizes.map { (stratum, size) ->
//            Sample(stratum, size, pickSample(size, casesByStratum[stratum]!!)) }
//
//        return PrimaryCaseSampleProvisional(samples)
        return TODO()
    }
}