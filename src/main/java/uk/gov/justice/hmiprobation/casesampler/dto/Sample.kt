package uk.gov.justice.hmiprobation.casesampler.dto

import uk.gov.justice.hmiprobation.casesampler.utils.Size.SampleSize
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class RowAssigner {
    var rowCount = AtomicInteger()

    fun nextId() = "${rowCount.incrementAndGet()}".padStart(3, '0')
}

data class PrimaryCaseSampleProvisional(val id: UUID, val timestamp: LocalDateTime, val results: List<StratumResult>) {

    constructor(results: List<Sample>) : this(UUID.randomUUID(), LocalDateTime.now(), toStratumResults(RowAssigner(), results))

    companion object {
        private fun toStratumResults(rowAssigner: RowAssigner, results: List<Sample>): List<StratumResult> =
                results.map {
                    StratumResult(
                        it.stratum,
                        it.size,
                        it.cases.map(toRow(rowAssigner)))
                }

        private fun toRow(assigner :RowAssigner): (Case) -> Row = { Row(assigner.nextId(), it) }
    }
}

data class Sample(val stratum: Stratum, val size: SampleSize, val cases: List<Case>)

data class Row(val row: String, val case: Case)

data class StratumResult(val stratum: Stratum, val size: SampleSize, val rows: List<Row>)


