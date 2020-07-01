package uk.gov.justice.hmiprobation.casesampler.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmiprobation.casesampler.controllers.EntityWithJwtAuthorisationBuilder
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisionalDetail
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = ["test"])
@DisplayName("Integration Tests for SampleController")
@Disabled
class AnalyzeSampleFile(
        @Autowired val testRestTemplate: TestRestTemplate,
        @Autowired val entityBuilder: EntityWithJwtAuthorisationBuilder,
        @Autowired val objectMapper: ObjectMapper) {

    @Test
    fun `Import file`() {

        FileConverter().run {
            convertSpreadsheetToJson(
                    filepath = "src/test/resources/yr 2 CRC Domain 2 case sample long list v0.1.xlsx",
                    sheetName = "CRC D2 Case Sample",
                    destination = "src/test/resources/sample.json"
            )
        }

        val json = this::class.java.getResource("/sample.json").readText()

        val response = testRestTemplate.exchange(
                "/analyse?size=100",
                HttpMethod.POST,
                entityBuilder.entityWithJwtAuthorisation("API_TEST_USER", roles = listOf("ROLE_PROBATION_CASE_SAMPLING"), body = json),
                String::class.java)

        File("src/test/resources/output.json").writeText(response.body!!, UTF_8)

        val sampleProvisionalDetail = objectMapper.readValue(response.body, PrimaryCaseSampleProvisionalDetail::class.java)

        with(sampleProvisionalDetail) {
            val totalCount = stratum.sumBy { it.size.count }

            val realPercentage: (Int, SampleSize) -> String = { total, group -> "%.2f".format(group.count.toDouble() / total * 100) }

            println("""
                Id:                      $id
                Timestamp:               $timestamp
                Total sample size:       $totalCount
                Stratum Summary:
            """.trimIndent())

            stratum.forEach {
                with(it) {
                    val stratumSize = "${it.size.count}/${it.size.total}".padEnd(8)
                    println("- name: ${name.name.padEnd(30)} size: ${stratumSize} (original: ${size.originalPercentage}%, actual: ${realPercentage(totalCount, it.size)}%)")
                }
            }

            println("\n\n")

            stratum.forEach { stratum ->
                val stratumSize = "${stratum.size.count}/${stratum.size.total}".padEnd(8)
                println("${stratum.name} count: $stratumSize")
                stratum.clusters.forEach { cluster ->
                    val size = "${cluster.size.count}/${cluster.size.total}".padEnd(8)
                    val actual = realPercentage(stratum.size.count, cluster.size)
                    println("- name: ${cluster.name.padEnd(30)} size: ${size} (original: ${cluster.size.originalPercentage}%, actual: $actual%)")
                }
                println("\n")
            }
        }
    }
}


