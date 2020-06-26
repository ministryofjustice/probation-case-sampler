package uk.gov.justice.hmiprobation.casesampler.controllers

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisionalDetail
import uk.gov.justice.hmiprobation.casesampler.dto.PrimaryCaseSampleProvisionalSummary
import uk.gov.justice.hmiprobation.casesampler.services.CaseSamplingService

@Api(tags = ["sampling"])
@RestController
@RequestMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE])

class SampleController(val caseSamplingService: CaseSamplingService) {

    @PostMapping(value = ["/sample"] ,consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(value = "Takes a Primary Case Sample and produces a Primary Case Sample Provisional")
    @PreAuthorize("hasRole('PROBATION_CASE_SAMPLING')")

    fun createSample(@RequestBody cases: List<Case>,
                     @ApiParam(value="Number of cases that should be present in the sample") @RequestParam(required = true) size: Int,
                     @ApiParam(value="Percentage of additional cases that should be added") @RequestParam(required = true, defaultValue = "20.00") buffer: Double
    ): PrimaryCaseSampleProvisionalSummary {
        val result = caseSamplingService.createSample(size, buffer, cases)
        return PrimaryCaseSampleProvisionalSummary(result)
    }

    @PostMapping(value = ["/analyse"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(value = "Takes a Primary Case Sample and produces a Primary Case Sample Provisional with stats")
    @PreAuthorize("hasRole('PROBATION_CASE_SAMPLING')")

    fun analyse(@RequestBody cases: List<Case>,
                @ApiParam(value="Number of cases that should be present in the sample") @RequestParam(required = true) size: Int,
                @ApiParam(value="Percentage of additional cases that should be added") @RequestParam(required = true, defaultValue = "20.00") buffer: Double
    ): PrimaryCaseSampleProvisionalDetail {
        val result = caseSamplingService.createSample(size, buffer, cases)
        return PrimaryCaseSampleProvisionalDetail(result)
    }
}
