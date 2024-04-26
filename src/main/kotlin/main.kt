import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.absolutePathString

@Serializable
data class Message(
    val role: String,
    val content: String,
)

@Serializable
data class PPLXRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Int,
)

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
    val body = RequestBody.create(mediaType, Json.encodeToString(data))

    val props = Properties()
    props.load(FileInputStream(".env"))
    val PPLX_KEY = props.getProperty("PPLX_API_KEY")

    val request = Request.Builder()
        .url("https://api.perplexity.ai/chat/completions")
        .post(body)
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
    }
    else {
        return Pair(Error("Failed to send message to PPLX"), "")
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun main() = session {
//    section { textLine("Hello, World") }.run()

    val systemMessage =
        """
            Given the Python code provided below, please check for and correct any syntax or runtime errors. Ensure the code runs without any errors after your modifications. This will be turned into a python file, so make it easy to parse back.  You may respond with the fixed code in the following format:  Fixed code: //Fixed Code.
        """.trimIndent()
    val userMessage = """
        # This program calculates the average of three numbers
        def calculate_average(num1, num2, num3):
            total = num1 + num2 + num3
            average = total / 3
            return average

        # Test the function
        result = calculate_average(10, 20, 30)
        print("The average is:", result)

        def check_passing(score):
            if score >= 50
                print("You passed!")
            else:
                print("You failed.")

        def greet(name):
            message = "Hello, " + name + "!"
            return message

        # Test the function
        greeting = greet("Alice")
        print(greeting)
    """.trimIndent()

    val (err, PPLXoutput) = sendMessageToPPLX(systemMessage, userMessage)

    println(PPLXoutput)
}