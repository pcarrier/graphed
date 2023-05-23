import com.pcarrier.graphed.Parser
import com.pcarrier.graphed.Printer
import java.io.File
import java.lang.StringBuilder

object CLI {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            runPrompt()
        } else {
            args.forEach { runFile(it) }
        }
    }

    private fun runFile(path: String) {
        val bytes = if (path == "-") {
            System.`in`.readAllBytes()
        } else {
            File(path).readBytes()
        }
        runString(String(bytes, Charsets.UTF_8))
    }

    private fun runPrompt() {
        main@ while (true) {
            val builder = StringBuilder()
            line@ while (true) {
                print("> ")
                val line = readlnOrNull() ?: break@main
                if (line.isEmpty()) {
                    break@line
                } else {
                    builder.appendLine(line)
                }
            }
            runString(builder.toString())
        }
    }

    private fun runString(str: String) {
        Parser().parse(str).let { Printer().print(it) }
    }
}
