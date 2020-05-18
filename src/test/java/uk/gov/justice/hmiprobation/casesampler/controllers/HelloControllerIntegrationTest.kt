package uk.gov.justice.hmiprobation.casesampler.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = ["test"])
@DisplayName("Integration Tests for HelloController")
class HelloControllerIntegrationTest(
        @Autowired val testRestTemplate: TestRestTemplate,
        @Autowired val entityBuilder: EntityWithJwtAuthorisationBuilder
) {

    @Nested
    @DisplayName("GET /hello")
    inner class GetMessage {
        @Test
        fun `Requires auth`() {
            val response =       testRestTemplate.getForEntity("/hello", String::class.java)
            with(response) {
                assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            }
        }

        @Test
        fun `returns message`() {
            val response =       testRestTemplate.exchange(
                    "/hello",
                    HttpMethod.GET,
                    entityBuilder.entityWithJwtAuthorisation(user = "API_TEST_USER", roles = listOf<String>()),
                    String::class.java)
            with(response) {
                assertThat(statusCode).isEqualTo(HttpStatus.OK)
                assertThat(body).isEqualTo("Hello world")
            }
        }
    }
}