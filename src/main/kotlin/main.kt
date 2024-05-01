package pyfixer

import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.Session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

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

object EnvVariables {
    val props = Properties().apply {
        if (File(".env").exists()) {
            load(FileInputStream(".env"))
        } else {
            System.getenv()["PPLX_API_KEY"]?.let {
                setProperty("PPLX_API_KEY", it)
            }
        }
    }.also {
        if (it["MAX_ALLOWED_ATTEMPTS"] == null) {
            it.setProperty("MAX_ALLOWED_ATTEMPTS", "5")
        }
    }
}

fun String.runCommand(workingDir: File? = null): Process {
    val processBuilder = ProcessBuilder(*this.split(" ").toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    if (workingDir != null) {
        processBuilder.directory(workingDir)
    }
    return processBuilder.start()
}

fun Session.attemptFix(attemptNo: Int, pythonFile: Path, dirPath: Path) {
    if (attemptNo > EnvVariables.props.getProperty("MAX_ALLOWED_ATTEMPTS").toInt()) {
        section {
            red()
            textLine("Exceeded maximum number of attempts. Exiting...")
            textLine("Unable to fix Python code, in the ${EnvVariables.props["MAX_ALLOWED_ATTEMPTS"]} attempts.")
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
    try {
        val PPLXmessage = sendMessageToPPLX(systemMessage, userMessage)
        handlePPLXOutput(PPLXmessage, attemptNo, pythonFile, dirPath)
    } catch (e: Exception) {
        section {
            red()
            textLine("Error: ${e.message}")
        }.run()
        return
    }
}

fun Session.handleFile(pythonFile: Path) {
    section {
        textLine("We will now attempt to fix the following Python file: ${pythonFile.pathString}")
        textLine("Please wait while we analyze the code...")
        textLine()
    }.run()

    if (Files.exists(Paths.get("PyFixer_Analyzer"))) {
        Files.walk(Paths.get("PyFixer_Analyzer"))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach { it.delete() }
    }
    Files.createDirectories(Paths.get("PyFixer_Analyzer"))
    val dirPath = Paths.get("PyFixer_Analyzer/")

    section {
        red()
        textLine("Errors found in python code: ")
    }.run()
    val process =
        "python -m py_compile ${pythonFile.absolutePathString()}".runCommand(File(dirPath.toString()))

    val exitCode = process.waitFor()

    if (exitCode == 1) {
        val PPLX_KEY = EnvVariables.props.getProperty("PPLX_API_KEY")
        if (PPLX_KEY == null) {
            section {
                red(); textLine("PPLX_API_KEY not found in .env file or system environment variables.")
                text("Enter a valid Perplexity.ai key or Ctrl-C to quit: "); input(viewMap = { '*' })
            }.runUntilInputEntered {
                onInputEntered {
                    EnvVariables.props.setProperty("PPLX_API_KEY", input)
                }
            }
        }
        attemptFix(1, pythonFile, dirPath)
        section {
            textLine("Writing to ${pythonFile.pathString}...")
            textLine(pythonFile.pathString)
        }.run()
        Files.writeString(pythonFile, Files.readString(Paths.get("$dirPath/output.py")))
    }
    section {
        green(); textLine("Python code is correct! Exiting...")
    }.run()

    // Clean up created directory
    Files.walk(dirPath)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach { it.delete() }
}

fun handlePath(path: String): Path {
    return if (path.startsWith("~")) {
        Paths.get("/home", System.getProperty("user.name"), path.drop(1))
    } else {
        Paths.get(path)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) = session {
    var pythonFile: Path? = null
    if (args.isEmpty()) {
        section {
            red()
            textLine("No arguments provided. Please provide the path to the Python file.")
            text("Enter the path to the Python file or Ctrl-C to quit: "); input()
        }.runUntilInputEntered {
            onInputEntered {
                pythonFile = handlePath(input)
                if (input == "" || !Files.exists(pythonFile!!)) {
                    rejectInput()
                }
            }
        }
    } else if (args.contains("--help") || args.contains("-h")) {
        section {
            textLine("Usage: pyfixer [options] [python file]")
            textLine()
            textLine("Options:")
            textLine("  -h, --help    Show this help message and exit")
            textLine()
            textLine("Arguments:")
            textLine("  python file    The path to the Python file you would like to fix")
        }.run()
        return@session
    } else {
        pythonFile = handlePath(args.last())
        if (!Files.exists(pythonFile!!)) {
            section {
                red()
                textLine("File does not exist. Please provide a valid path.")
                text("Enter the path to the Python file or Q to quit: ")
                input(); white()
            }.runUntilInputEntered {
                onInputEntered {
                    pythonFile = handlePath(input)
                    if (input == "" || !Files.exists(pythonFile!!)) {
                        rejectInput()
                    }
                }
            }
        }
    }
    handleFile(pythonFile!!)
    section {
        textLine("Thank you for using PyFixer!")
    }.run()
}
