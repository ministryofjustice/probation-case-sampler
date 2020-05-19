package uk.gov.justice.hmiprobation.casesampler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CaseSamplerApplication

fun main(args: Array<String>) {
  runApplication<CaseSamplerApplication>(*args)
}
