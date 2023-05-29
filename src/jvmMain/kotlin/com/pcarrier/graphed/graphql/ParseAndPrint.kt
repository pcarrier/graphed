package com.pcarrier.graphed.graphql

object ParseAndPrint {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = System.`in`.bufferedReader().readLine()
        val doc = Parser(src).parse()
        val printed = Printer().printDocument(doc)
        println(printed)
    }
}
