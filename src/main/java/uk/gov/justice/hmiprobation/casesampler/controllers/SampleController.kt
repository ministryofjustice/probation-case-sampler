package uk.gov.justice.hmiprobation.casesampler.controllers

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisionalDetail
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisionalSummary
import uk.gov.justice.hmiprobation.casesampler.services.CaseListService

@Api(tags = ["sampling"])
@RestController
@RequestMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE])

class SampleController(val caseListService: CaseListService,
                       @Value("\${sample.buffer.size:20.00}") val buffer: Double) {

    @PostMapping(value = ["/sample"] ,consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(value = "Takes a Primary Case Sample and produces a Primary Case Sample Provisional")
    fun createSample(@RequestBody cases: List<Case>,
                     @ApiParam(value="Number of cases that should be present in the sample") @RequestParam(required = true) size: Int): PrimaryCaseSampleProvisionalSummary {
        val result = caseListService.process(size, buffer, cases)
        return PrimaryCaseSampleProvisionalSummary(result)
    }

    @PostMapping(value = ["/analyse"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(value = "Takes a Primary Case Sample and produces a Primary Case Sample Provisional with stats")
    fun analyse(@RequestBody cases: List<Case>,
                @ApiParam(value="Number of cases that should be present in the sample") @RequestParam(required = true) size: Int): PrimaryCaseSampleProvisionalDetail {
        val result = caseListService.process(size, buffer, cases)
        return PrimaryCaseSampleProvisionalDetail(result)
    }
}
