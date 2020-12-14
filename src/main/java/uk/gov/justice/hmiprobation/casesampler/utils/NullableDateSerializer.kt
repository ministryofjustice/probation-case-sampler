package uk.gov.justice.hmiprobation.casesampler.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NullableDateSerializer constructor(vc: Class<*>? = null) : StdDeserializer<LocalDate?>(vc) {
  val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): LocalDate? {
    val nextVal = jp.text
    return if (nextVal == "N") null else LocalDate.parse(nextVal, dateFormatter)
  }
}
