package uk.gov.justice.hmiprobation.casesampler.services

import org.springframework.stereotype.Service
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSample
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisional
import uk.gov.justice.hmiprobation.casesampler.utils.calculateSampleSize

@Service
class CaseListService(val samplePicker: SamplePicker = SamplePicker()) {

    fun process(sampleSize: Int, bufferPercentage: Double, longlist: PrimaryCaseSample): PrimaryCaseSampleProvisional {

        val casesByStratum = longlist.asSequence()
                .filter { !it.isExcluded }
                .filter { it.isEarliestCaseForOffender(longlist) }
                .groupBy { it.getStratum() }

        val sampleSizes = calculateSampleSize(sampleSize, casesByStratum, bufferPercentage)

        val samples = samplePicker.pick(sampleSizes, casesByStratum)

        return PrimaryCaseSampleProvisional(samples)
    }
}