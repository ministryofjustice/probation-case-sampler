package uk.gov.justice.hmiprobation.casesampler.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import uk.gov.justice.hmiprobation.casesampler.services.AllocationData
import uk.gov.justice.hmiprobation.casesampler.services.Info
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
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
          it.cases.map(toRow(rowNumberAssigner))
        )
      }

    private fun toRow(numberAssigner: RowNumberAssigner): (Case) -> Row<Case> = { Row(numberAssigner.nextId(), it) }
  }
}

data class Sample(val stratum: Stratum, val size: SampleSize, val allocationData: List<AllocationData>, val cases: List<Case>)

data class Row<T>(val row: String, @field:JsonUnwrapped val payload: T)

data class StratumResult(val stratum: Stratum, val size: SampleSize, val allocationData: List<AllocationData>, val rows: List<Row<Case>>)

data class PrimaryCaseSampleProvisionalSummary(val id: UUID, val timestamp: LocalDateTime, val results: List<Row<Case>>) {
  constructor(data: PrimaryCaseSampleProvisional) : this(data.id, data.timestamp, data.results.flatMap { it.rows })
}

data class PrimaryCaseSampleProvisionalDetail(
  val id: UUID,
  val timestamp: LocalDateTime,
  val results: List<Row<Case>>,
  val stratum: List<StratumDto>
) {
  constructor(data: PrimaryCaseSampleProvisional) : this(
    data.id,
    data.timestamp,
    data.results.flatMap { it.rows },
    data.results.map { StratumDto(it) }
  )
}

data class StratumDto(val name: Stratum, val size: SampleSize, val clusters: List<ClusterDto>) {
  constructor(result: StratumResult) : this(
    result.stratum,
    result.size,
    result.allocationData.groupBy { it.cluster }.map { (info, allocationData) -> ClusterDto(info, allocationData) }
  )
}

data class ClusterDto(val name: String, val size: SampleSize, val ldus: List<LduDto>) {
  constructor(info: Info, allocationData: List<AllocationData>) : this(
    info.id,
    info.size,
    allocationData.groupBy { it.ldu }.map { (info, allocationData) -> LduDto(info, allocationData) }
  )
}

data class LduDto(val name: String, val size: SampleSize, val ros: List<RoDto>) {
  constructor(info: Info, allocationData: List<AllocationData>) : this(
    info.id,
    info.size,
    allocationData.map { RoDto(it) }
  )
}

data class RoDto(val name: String, val size: SampleSize) {
  constructor(id: AllocationData) : this(id.ro.id, id.ro.size)
}
