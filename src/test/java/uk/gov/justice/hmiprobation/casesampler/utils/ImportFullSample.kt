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
                entityBuilder.entityWithJwtAuthorisation("API_TEST_USER", body = json),
                String::class.java)

        File("src/test/resources/output.json").writeText(response.body!!, UTF_8)

        val sampleProvisionalDetail = objectMapper.readValue(response.body, PrimaryCaseSampleProvisionalDetail::class.java)

        with (sampleProvisionalDetail) {
            println("""
                Id:                            ${id}
                Timestamp:                     $timestamp
                Total requested sample size:   ${stratum.sumBy { it.size.count }}
                Stratum Summary:
            """.trimIndent())

            stratum.forEach {
                with (it) {
                    println("""
                        - name: ${name.name.padEnd(30)} size: ${size.count.toString().padEnd(5)} (${size.originalPercentage})
                    """.trimIndent())
                }
            }

            println("\n\n")

            stratum.forEach {
                with (it) {
                    println("$name:")
                    it.clusters.forEach {
                        println("- name: ${it.name.padEnd(30)} size: ${it.size.count.toString().padEnd(5)} (${it.size.originalPercentage})")
                    }
                    println("\n")
                }
            }
        }
    }

}

