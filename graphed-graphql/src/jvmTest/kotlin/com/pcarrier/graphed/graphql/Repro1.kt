package com.pcarrier.graphed.graphql

import graphql.language.AstPrinter
import graphql.parser.ParserEnvironment
import graphql.parser.ParserOptions

object Repro1 {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = "\"\\\"\" scalar A"

        println("=== Original ===")
        println(src)
        val env = ParserEnvironment.newParserEnvironment()
            .document(src)
            .parserOptions(
                ParserOptions.newParserOptions()
                    .captureIgnoredChars(true)
                    .build()
            )
            .build()
        val doc = graphql.parser.Parser.parse(env)
        val printed = AstPrinter.printAst(doc)
        println("=== Parsed and printed ===")
        println(printed)
        graphql.parser.Parser.parse(printed)
    }
}
