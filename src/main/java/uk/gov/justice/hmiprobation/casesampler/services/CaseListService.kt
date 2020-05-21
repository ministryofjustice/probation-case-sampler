package uk.gov.justice.hmiprobation.casesampler.services

import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.utils.calculateSampleSize

class CaseListService() {

    /**
     * @param longlist a primary case sample
     * @return shortlist (a Primary Case Sample Provisional)
     */
    fun process(sampleSize: Int, longlist: List<Case>) {

        val groupedByStratification = longlist.asSequence()
                .filter { it.isExcluded() }
                .filter { it.isEarliestCaseForOffender(longlist) }
                .groupBy { it.getStratification() }

        val sampleSizes = calculateSampleSize(sampleSize, groupedByStratification)

//        Randomly select cases in stratifications whilst maintaining proportions of
//
//        Cluster
//
//        Local Delivery Unit (LDU)
//
//        Responsible Officer (RO) up to maximum of n
//
//                sample size and additional % are configurable (default to 120 and 20% respectively)
//
//        Assign the sample a uuid and timestamp
//
//        Assign each line an id starting from 001
//
    }

}