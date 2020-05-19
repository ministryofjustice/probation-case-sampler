package uk.gov.justice.hmiprobation.casesampler.utils

object UserContext {
    private val authToken = ThreadLocal<String>()
    fun getAuthToken(): String {
        return authToken.get()
    }

    fun setAuthToken(aToken: String?) {
        authToken.set(aToken)
    }
}