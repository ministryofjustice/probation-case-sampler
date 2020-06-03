package uk.gov.justice.hmiprobation.casesampler.controllers

import com.nhaarman.mockitokotlin2.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.json.BasicJsonTester
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.Gender
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisional
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel
import uk.gov.justice.hmiprobation.casesampler.dto.Row
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType
import uk.gov.justice.hmiprobation.casesampler.dto.Stratum.MALE_POST_CUSTODY_NON_LOW
import uk.gov.justice.hmiprobation.casesampler.dto.StratumResult
import uk.gov.justice.hmiprobation.casesampler.services.AllocationData
import uk.gov.justice.hmiprobation.casesampler.services.CaseListService
import uk.gov.justice.hmiprobation.casesampler.services.Info
import uk.gov.justice.hmiprobation.casesampler.utils.SampleSize
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = ["test"])
@DisplayName("Integration Tests for SampleController")
class SampleControllerIntegrationTest(
        @Autowired val testRestTemplate: TestRestTemplate,
        @Autowired val entityBuilder: EntityWithJwtAuthorisationBuilder
) {

    @MockBean
    lateinit var caseListService: CaseListService

    val jsonTester = BasicJsonTester(this.javaClass)

    val case = Case("BK", "BK", "13/11/1981", Gender.MALE, SentenceType.POST_CUSTODY,
            "R11111", "2000/0123456Q", RiskOfSeriousHarmLevel.MEDIUM, LocalDate.of(2018, 11, 9),
            LocalDate.of(2019, 2, 26),
            "Westeros", "Winterfell", "Stark", "ZN", "XA", "Ned")

    @BeforeEach
    fun setUp() {
        Mockito.reset(caseListService)

        `when`(caseListService.process(any(), any(), any())).thenReturn(PrimaryCaseSampleProvisional(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                timestamp = LocalDateTime.of(2020, 1, 2, 12, 30),
                results = listOf(StratumResult(
                        MALE_POST_CUSTODY_NON_LOW,
                        SampleSize(1 ,1, "100.00"),
                        listOf(
                                AllocationData(
                                        cluster = Info("Westeros", SampleSize(1, 1, "100.00")),
                                        ldu = Info("Winterfell", SampleSize(1, 1, "100.00")),
                                        ro = Info("ZN", SampleSize(1, 1, "100.00"))
                                )),
                        listOf(Row("001", case))))
        ))
    }

    @Test
    fun `create sample`() {
        val response = testRestTemplate.exchange(
                "/sample?size=10",
                HttpMethod.POST,
                entityBuilder.entityWithJwtAuthorisation("API_TEST_USER", body = """
                       [{
                          "familyName" : "BK",
                          "firstName" : "BK",
                          "dob" : "13/11/1981",
                          "gender" : "M",
                          "sentenceType" : "POST_CUSTODY",
                          "crn" : "R11111",
                          "pnc" : "2000/0123456Q",
                          "roshClassification" : "MEDIUM",
                          "startDate" : "09/11/2018",
                          "endDate" : "26/02/2019",
                          "cluster" : "Westeros",
                          "ldu" : "Winterfell",
                          "team" : "Stark",
                          "responsibleOfficer" : "ZN",
                          "manager" : "XA",
                          "officer" : "Ned"
                        }]
                    """".trimIndent()),
                String::class.java)

        with(response) {
            assertThat(statusCode).isEqualTo(HttpStatus.OK)
            verify(caseListService).process(10, 20.0, listOf(
                    case
            ))
            assertThat(jsonTester.from(body)).isStrictlyEqualToJson("result-summary.json")
        }
    }

    @Test
    fun `call Service with detail`() {
        val response = testRestTemplate.exchange(
                "/analyse?size=10",
                HttpMethod.POST,
                entityBuilder.entityWithJwtAuthorisation("API_TEST_USER", body = """
                       [{
                          "familyName" : "BK",
                          "firstName" : "BK",
                          "dob" : "13/11/1981",
                          "gender" : "M",
                          "sentenceType" : "POST_CUSTODY",
                          "crn" : "R11111",
                          "pnc" : "2000/0123456Q",
                          "roshClassification" : "MEDIUM",
                          "startDate" : "09/11/2018",
                          "endDate" : "26/02/2019",
                          "cluster" : "Westeros",
                          "ldu" : "Winterfell",
                          "team" : "Stark",
                          "responsibleOfficer" : "ZN",
                          "manager" : "XA",
                          "officer" : "Ned"
                        }]
                    """".trimIndent()),
                String::class.java)

        with(response) {
            assertThat(statusCode).isEqualTo(HttpStatus.OK)
            verify(caseListService).process(10, 20.0, listOf(
                    case
            ))
            assertThat(jsonTester.from(body)).isStrictlyEqualToJson("result-detail.json")
        }
    }

}