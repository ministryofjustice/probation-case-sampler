package uk.gov.justice.hmiprobation.casesampler.dto

import uk.gov.justice.hmiprobation.casesampler.services.AllocationData
import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class RowNumberAssigner {
    var rowCount = AtomicInteger()

    fun nextId() = "${rowCount.incrementAndGet()}".padStart(3, '0')
}

data class PrimaryCaseSampleProvisional(val id: UUID, val timestamp: LocalDateTime, val results: List<StratumResult>) {

    constructor(results: List<Sample>) : this(UUID.randomUUID(), LocalDateTime.now(), toStratumResults(RowNumberAssigner(), results))

    companion object {
        private fun toStratumResults(rowNumberAssigner: RowNumberAssigner, results: List<Sample>): List<StratumResult> =
                results.map {
                    StratumResult(
                            it.stratum,
                            it.size,
                            it.allocationData,
                            it.cases.map(toRow(rowNumberAssigner)))
                }

        private fun toRow(numberAssigner: RowNumberAssigner): (Case) -> Row = { Row(numberAssigner.nextId(), it) }
    }
}

data class Sample(val stratum: Stratum, val size: SampleSize, val allocationData: List<AllocationData>, val cases: List<Case>)

data class Row(val row: String, val case: Case)

data class StratumResult(val stratum: Stratum, val size: SampleSize, val allocationData: List<AllocationData>, val rows: List<Row>)


