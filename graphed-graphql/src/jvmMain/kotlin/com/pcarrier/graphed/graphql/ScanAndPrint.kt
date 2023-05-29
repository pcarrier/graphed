package com.pcarrier.graphed.graphql

object ScanAndPrint {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = String(System.`in`.readAllBytes())
        val scanner = Scanner(src)
        while (true) {
            val tok = scanner.scan()
            if (tok is Token.EndOfFile) break
            println(tok)
        }
    }
}
