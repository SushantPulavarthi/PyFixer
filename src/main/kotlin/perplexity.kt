package pyfixer

import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.runtime.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

val systemMessage =
    """
            Given the Python code provided below, please check for and correct any syntax or runtime errors.
            Ensure the code runs without any errors after your modifications.
            Make sure to fix any syntax errors, and try your best to fix runtime errors. For example: wrap in a try-except block, etc.
            Do not change the logic of the code, and do not remove any lines. You must return the entire code back to the user, and not add any comments.
            You may only respond in the following format, and nothing else:
            Fixed code: 
            ```python
            // Fixed Code
            ```
            Explanation:
            // Explanation of the changes made. And actions that need to be taken care of by user (eg: runtime errors)
        """.trimIndent()

@OptIn(ExperimentalSerializationApi::class)
fun sendMessageToPPLX(systemMessage: String, userMessage: String): String {
    val client = OkHttpClient()
    val mediaType = "application/json".toMediaType()

    val data = PPLXRequest(
        model = "mistral-7b-instruct",
        messages = listOf(
            Message(role = "system", content = systemMessage),
            Message(role = "user", content = userMessage),
        ),
        temperature = 0,
    )
    val request = Request.Builder()
        .url("https://api.perplexity.ai/chat/completions")
        .post(Json.encodeToString(data).toRequestBody(mediaType))
        .addHeader("accept", "application/json")
        .addHeader("content-type", "application/json")
        .addHeader("authorization", "Bearer ${EnvVariables.props.getProperty("PPLX_API_KEY")}")
        .build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string()

    if (response.code == 200) {
        val output = Json.parseToJsonElement(responseBody!!).jsonObject
        val choices = output["choices"]!!.jsonArray
        val choice = choices[0].jsonObject["message"]!!.jsonObject["content"]
        return choice.toString()
    } else {
        throw Exception("Failed to send message to Perplexity.ai. Error: $responseBody")
    }
}

fun Session.handlePPLXOutput(PPLXoutput: String, attemptNo: Int, pythonFile: Path, dirPath: Path) {
    val split = PPLXoutput.replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .split("Explanation:")

    val fixedCode = split[0].substringAfter("```python").substringBefore("```").trim()
    val explanation = split[1].trim()
    val outputPythonFile = Paths.get("$dirPath/output.py")
    Files.writeString(outputPythonFile, fixedCode)

    section {
        green(); textLine("Fixed code:")
        white(); textLine(fixedCode); textLine()
        red(); textLine("Explanation:")
        white(); textLine(explanation)
    }.run()

    section {
        red()
        textLine("Errors found: ")
    }.run()
    val process =
        "python -m py_compile ${outputPythonFile.absolutePathString()}".runCommand(File(dirPath.toString()))
    val exitCode = process.waitFor()

    if (exitCode == 1) {
        attemptFix(attemptNo + 1, pythonFile, dirPath)
    }
}
