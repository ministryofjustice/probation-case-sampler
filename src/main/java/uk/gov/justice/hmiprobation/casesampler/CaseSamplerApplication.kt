package uk.gov.justice.hmiprobation.casesampler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class ProbationTeamsApplication

fun main(args: Array<String>) {
  runApplication<ProbationTeamsApplication>(*args)
}
