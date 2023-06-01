package com.pcarrier.graphed.graphql

import kotlin.test.Test
import kotlin.test.assertEquals

class ScannerTest {
    @Test
    fun blockString() {
        val strings = listOf(
            "\"\"\"abc\n\ndef\"\"\"",
            "\"\"\"abc\n    \n  def\"\"\"",
            "\"\"\"abc\n  \ndef\n\n\"\"\""
        )
        strings.forEachIndexed { i, s ->
            val scanner = Scanner(s)
            val result = scanner.remaining()
            assertEquals(2, result.size, "Wrong token count in $i")
            assertEquals("abc\n\ndef", (result[0] as Token.String).value, "Wrong block string in $i")
        }
    }
}
