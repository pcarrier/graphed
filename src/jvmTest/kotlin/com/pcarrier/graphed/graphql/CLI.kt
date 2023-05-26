package com.pcarrier.graphed.graphql

import graphql.language.AstPrinter
import graphql.parser.ParserEnvironment
import graphql.parser.ParserOptions
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.io.File

object CLI {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            runPrompt()
        } else {
            args.forEach { runFile(it, "$it.graphed") }
        }
    }

    private fun runFile(path: String, out: String) {
        val bytes = if (path == "-") {
            System.`in`.readAllBytes()
        } else {
            File(path).readBytes()
        }
        runSource(String(bytes, Charsets.UTF_8), out)
    }

    private fun runPrompt() {
        val term = TerminalBuilder.builder().system(true).build()
        val lr = LineReaderBuilder.builder().terminal(term).build()
        term.writer().println("Empty line to proceed, <<<file to read from a file.")
        main@ while (true) {
            try {
                val builder = StringBuilder()
                line@ for (n in 0..Int.MAX_VALUE) {
                    val line = try {
                        lr.readLine("${n}> ")
                    } catch (_: Exception) {
                        break@main
                    }
                    if (line.isEmpty()) {
                        break@line
                    } else if (line.startsWith("<<<")) {
                        builder.append(File(line.substring(3)).readText())
                    } else {
                        builder.appendLine(line)
                    }
                }
                val dest = try {
                    lr.readLine("dest> ")
                } catch (e: Exception) {
                    null
                }
                runSource(builder.toString(), dest)
            } catch (e: Exception) {
                System.err.println("${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun <V> timed(name: String, lambda: () -> V): V {
        val started = System.nanoTime()
        return lambda().also {
            val took = System.nanoTime() - started
            System.err.printf("%15s %9fs\n", name, took.toDouble() / 1e9)
        }
    }

    private fun runSource(str: String, dest: String?) {
        val doc = timed("kotlin parse") { Parser(str).parse() }
        val javaDoc = timed("java parse") {
            graphql.parser.Parser.parse(
                ParserEnvironment.newParserEnvironment()
                    .document(str)
                    .parserOptions(
                        ParserOptions.newParserOptions()
                            .captureIgnoredChars(true)
                            .captureSourceLocation(false)
                            .captureLineComments(false)
                            .maxCharacters(Int.MAX_VALUE)
                            .maxWhitespaceTokens(Int.MAX_VALUE)
                            .maxTokens(Int.MAX_VALUE)
                            .maxRuleDepth(Int.MAX_VALUE)
                            .build()
                    )
                    .build()
            )
        }
        val printed = timed("kotlin print") { Printer.printDocument(doc) }
        val sexp = timed("sexp") { ToSexp.document(doc).toString() }
        val javaPrinted = timed("java print") { AstPrinter.printAst(javaDoc) }
        System.err.println("prints to ${printed.length} (java ${javaPrinted.length}), sexp to ${sexp.length}")
        if (dest != null) {
            val dir = File(dest)
            dir.mkdirs()
            dir.resolve("kotlin.graphql").writeText(printed)
            dir.resolve("java.graphql").writeText(javaPrinted)
            dir.resolve("sexp").writeText(sexp)
        }
    }
}
