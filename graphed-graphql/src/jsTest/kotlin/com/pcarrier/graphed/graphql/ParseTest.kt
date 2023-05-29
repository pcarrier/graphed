package com.pcarrier.graphed.graphql

import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class ParseTest {
    val readFileSync = js("function (f) { return require('fs').readFileSync(f, 'utf8'); }")

    @Test
    fun parseAndPrint() {
        val src = readFileSync("../../../../onegraph.graphql")
        repeat(5) {
            measureTime {
                Parser(src).parse()
            }.also { println(it) }
        }
    }
}
