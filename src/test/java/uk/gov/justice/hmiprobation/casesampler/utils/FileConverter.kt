package uk.gov.justice.hmiprobation.casesampler.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import uk.gov.justice.hmiprobation.casesampler.dto.Case
import uk.gov.justice.hmiprobation.casesampler.dto.Gender
import uk.gov.justice.hmiprobation.casesampler.dto.RiskOfSeriousHarmLevel
import uk.gov.justice.hmiprobation.casesampler.dto.SentenceType
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.ZoneId


@JsonIgnoreProperties(ignoreUnknown = true)
abstract class SpreadsheetCase(
        @JsonProperty("Family name") var familyName: String,
        @JsonProperty("First name(s)") var firstName: String,
        @JsonProperty("DoB") val dob: String,
        @JsonProperty("Gender") val gender: Gender,
        @JsonProperty("Case/sentence type [Licence] [Community Order] [Suspended Sentence Order]") val sentenceType: SentenceType,
        @JsonProperty("CRN") val crn: String,
        @JsonProperty("PNC") val pnc: String,
        @JsonProperty("Risk of Serious Harm [RoSH] Classification") val roshClassification: RiskOfSeriousHarmLevel,
        @JsonFormat(pattern = "dd/MM/yyyy")
        @JsonProperty("Date of sentence or release on licence") val startDate: LocalDate,
        @JsonDeserialize(using = NullableDateSerializer::class)
        @JsonFormat(pattern = "dd/MM/yyyy")
        @JsonProperty("Order or licence terminated[Date/N]") val endDate: LocalDate,
        @JsonProperty("Cluster(as recorded on nDelius)") val cluster: String,
        @JsonProperty("LDU(as recorded on nDelius)") val ldu: String,
        @JsonProperty("Team(as recorded on nDelius)") val team: String,
        @JsonProperty("Responsible officer") val responsibleOfficer: String,
        @JsonProperty("Manager") val manager: String,
        @JsonProperty("Office") val officer: String
)

class FileConverter {

    val readMapper = ObjectMapper()
            .addMixIn(Case::class.java, SpreadsheetCase::class.java)
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())

    val writeMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .setVisibility(PropertyAccessor.ALL, Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)

    private fun readSheet(filepath: String, sheetName: String): List<Case> {
        val inputStream = FileInputStream(filepath)
        val xlWb = WorkbookFactory.create(inputStream)

        val sheet = xlWb.getSheet(sheetName)

        val iterator = sheet.rowIterator()

        // skip first row
        iterator.next()

        val colNames = extractColNames(iterator.next()!!)

        return iterator.asSequence()
                .takeWhile { it.getCell(0)?.stringCellValue?.isNotEmpty() ?: false }
                .map { extractValues(it!!) }
                .map { colNames.zip(it).toMap()  }
                .map { readMapper.convertValue(it, Case::class.java) }
                .toList()
    }

    private fun readCell(cell: Cell): Any = when (cell.cellTypeEnum) {

        CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell))
            cell.dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        else cell.numericCellValue

        CellType.STRING -> cell.stringCellValue.trim()

        CellType.BOOLEAN -> cell.booleanCellValue
        else -> ""
    }

    private fun extractValues(row: Row) = row.cellIterator().iterator().asSequence().map { readCell(it!!) }.toList()

    private fun extractColNames(row: Row): List<String> = row.cellIterator().iterator().asSequence()
            .map { it?.stringCellValue?.trim() ?: "" }
            .map { it.replace("\n", "") }
            .map { it.replace("\\s+".toRegex(), " ") }
            .toList()

    private fun writeToJsonFile(cases: List<Case>, fileName: String) = writeMapper.writerWithDefaultPrettyPrinter().writeValue(File(fileName), cases)

    fun convertSpreadsheetToJson(filepath: String, sheetName: String, destination: String) {
        val cases = readSheet(filepath, sheetName)
        writeToJsonFile(cases, destination)
    }
}