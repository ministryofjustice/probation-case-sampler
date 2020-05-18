package uk.gov.justice.hmiprobation.casesampler.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse @JsonCreator constructor(val status: Int, val developerMessage: String? = null)