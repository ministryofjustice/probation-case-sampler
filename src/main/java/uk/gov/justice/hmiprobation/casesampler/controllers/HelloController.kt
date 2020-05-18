package uk.gov.justice.hmiprobation.casesampler.controllers

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api(tags = ["sampling"])
@RestController
@RequestMapping(
        value = ["hello"],
        produces = [MediaType.APPLICATION_JSON_VALUE])

class HelloController() {

    @GetMapping
    @ApiOperation(value = "Retrieve message")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "OK", response = String::class)
    ])
    fun getProbationAreaCodes(): String = "Hello world"
}