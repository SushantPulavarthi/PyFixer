package pyfixer

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import onFailure
import onSuccess
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

val props = Properties().apply {
    load(FileInputStream(".env"))
}
val PPLX_KEY: String = props.getProperty("PPLX_API_KEY")
const val MAX_ALLOWED_ATTEMPTS = 5

val systemMessage =
    """
            Given the Python code provided below, please check for and correct any syntax or runtime errors.
            Ensure the code runs without any errors after your modifications.
            Make sure to fix any syntax errors, and try your best to fix runtime errors. For example: wrap in a try-except block, etc.
            Do not change the logic of the code, and do not remove any lines. You must return the entire code back to the user.
            You may only respond in the following format, and nothing else:
            Fixed code: 
            ```python
            // Fixed Code
            ```
            Explanation:
            // Explanation of the changes made. And actions that need to be taken care of by user (eg: runtime errors)
        """.trimIndent()

@OptIn(ExperimentalSerializationApi::class)
fun sendMessageToPPLX(systemMessage: String, userMessage: String): Pair<Error?, String> {
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
        .post(RequestBody.create(mediaType, Json.encodeToString(data)))
        .addHeader("accept", "application/json")
        .addHeader("content-type", "application/json")
        .addHeader("authorization", "Bearer $PPLX_KEY")
        .build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string()

    if (response.code == 200) {
        val output = Json.parseToJsonElement(responseBody!!).jsonObject
        val choices = output["choices"]!!.jsonArray
        val choice = choices[0].jsonObject["message"]!!.jsonObject["content"]
        return Pair(null, choice.toString())
    } else {
        return Pair(Error("Failed to send message to PPLX"), "")
    }
}