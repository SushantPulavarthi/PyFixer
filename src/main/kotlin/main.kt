package pyfixer

import java.util.*
import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import onFailure
import onSuccess
import runCommand
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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


fun Session.attemptFix(attemptNo: Int, pythonFile: Path, dirPath: Path) {
    if (attemptNo > MAX_ALLOWED_ATTEMPTS) {
        section {
            red()
            textLine("Exceeded maximum number of attempts. Exiting...")
            textLine("Unable to fix Python code, in the given number of attempts. Please try again later.")
        }.run()
        return
    }
    section {
        textLine("Sent code to PPLX for analysis. Waiting for response...")
        if (attemptNo > 1) {
            textLine("Attempt $attemptNo")
        }
    }.run()

    val userMessage = Files.readString(pythonFile)
    val (err, PPLXoutput) = sendMessageToPPLX(systemMessage, userMessage)


    if (err != null) {
        section {
            red()
            textLine(err.message.toString())
        }.run()
        return
    } else {
        // Split the output into the fixed code and the explanation and clean escape characters
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
            green()
            textLine("Fixed code:")
            white()
            textLine(fixedCode)
            red()
            textLine("Explanation:")
            white()
            textLine(explanation)
        }.run()

        val process = testPython(outputPythonFile, dirPath)
        val exitCode = process.waitFor()

        process.onFailure {
            attemptFix(attemptNo + 1, pythonFile, dirPath)
        }
//        process.onSuccess {
//            section {
//                green()
//                textLine("Python code is correct! Exiting...")
//            }.run()
//        }
    }
}

fun testPython(pythonPath: Path, dirPath: Path): Process {
    val hello = "source $dirPath/venv/bin/activate && python -m py_compile ${pythonPath.absolutePathString()}".runCommand()
    return hello
}

@OptIn(ExperimentalSerializationApi::class)
fun main() = session {

    val pythonFile = Paths.get("test.py")

    if (!Files.exists(Paths.get("PyFixer_Analyzer"))) {
        Files.createDirectory(Paths.get("PyFixer_Analyzer"))
    }
    val dirPath = Paths.get("PyFixer_Analyzer/")
    "python3 -m venv $dirPath/venv".runCommand()
    var process = testPython(pythonFile, dirPath)

    process.onFailure {
        // Need to run the process again
        val props = Properties().apply {
            load(FileInputStream(".env"))
        }
        val PPLX_KEY = props.getProperty("PPLX_API_KEY")
        if (PPLX_KEY == null) {
            section {
                red()
                textLine("PPLX_API_KEY not found in .env file")
            }.run()
            return@onFailure
        }
        attemptFix(1, pythonFile, dirPath)
        Files.writeString(pythonFile, Files.readString(Paths.get("$dirPath/output.py")))
    }
    section {
        green()
        textLine("Python code is correct! Exiting...")
    }.run()

    // Clean up created directory
    Files.walk(dirPath)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach { it.delete() }
}