package com.pcarrier.graphed.graphql

object ParseAndPrint {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = String(System.`in`.readAllBytes())
        val doc = Parser(src).parse()
        val printed = Printer().printDocument(doc)
        println(printed)
    }
}
