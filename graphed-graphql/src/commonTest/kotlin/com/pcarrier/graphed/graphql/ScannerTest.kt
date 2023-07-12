package com.pcarrier.graphed.graphql

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScannerTest {
    @Test
    fun blockString() {
        val strings = listOf(
            "\"\"\"abc\n\ndef\"\"\"",
            //"\"\"\"abc\n    \n  def\"\"\"",
            //"\"\"\"abc\n  \ndef\n\n\"\"\"",
            //"    \"\"\"abc\n  \ndef\n   \n    \"\"\""
        )
        strings.forEachIndexed { i, s ->
            val scanner = Scanner(s)
            val result = scanner.remaining()
            assertEquals(2, result.size, "Wrong token count in $i")
            assertEquals("abc\n\ndef", (result[0] as Token.String).value, "Wrong block string in $i")
        }
    }

    private fun scanFirst(string: String): Token {
        return Scanner(string).run {
            scan()
        }
    }

    private fun scanSecond(string: String): Token {
        return Scanner(string).run {
            scan()
            scan()
        }
    }

    private fun Token.assertName(value: String) {
        assertIs<Token.Name>(this)
        assertEquals(value, this.value)
    }

    private fun Token.assertString(value: String) {
        assertIs<Token.String>(this)
        assertEquals(value, this.value)
    }

    @Test
    fun ignoresBOMHeader() {
        scanFirst("\uFEFF foo").apply {
            assertName("foo")
        }
    }

    @Test
    fun tracksLineBreaks() {
        scanFirst("foo").apply {
            assertName("foo")
        }
        scanFirst("\nfoo").apply {
            assertName("foo")
        }
        scanFirst("\n\rfoo").apply {
            assertName("foo")
        }
        scanFirst("\r\r\n\nfoo").apply {
            assertName("foo")
        }
        scanFirst("\n\n\r\rfoo").apply {
            assertName("foo")
        }
    }


    @Test
    fun skipsWhitespaceAndComments() {
        scanFirst(
            """

              foo


              """
        ).apply {
            assertName("foo")
        }

        scanFirst("\t\tfoo\t\t").apply {
            assertName("foo")
        }

        scanFirst(
            """
      #comment
      foo#comment
    """
        ).apply {
            assertName("foo")
        }

        scanFirst(",,,foo,,,").apply {
            assertName("foo")
        }
    }

    @Test
    fun scansStrings() {
        scanFirst("\"\"").apply {
            assertString("")
        }
        scanFirst("\"simple\"").apply {
            assertString("simple")
        }
        scanFirst("\" white space \"").apply {
            assertString(" white space ")
        }
        scanFirst("\"quote \\\"\"").apply {
            assertString("quote \"")
        }
        scanFirst("\"escaped \\n\\r\\b\\t\\f\"").apply {
            assertString("escaped \n\r\b\t\u000c")
        }
        scanFirst("\"slashes \\\\ \\/\"").apply {
            assertString("slashes \\ /")
        }
        // 😀is 0x1f600 or \uD83D\uDE00 surrogate pair
        scanFirst("\"unescaped unicode outside BMP 😀\"").apply {
            assertString("unescaped unicode outside BMP 😀")
        }
        // 􏿿 is 0x10FFFF or \uDBFF\uDFFF surrogate pair
        scanFirst("\"unescaped maximal unicode outside BMP 􏿿\"").apply {
            assertString("unescaped maximal unicode outside BMP \uDBFF\uDFFF") //
        }
        scanFirst("\"unicode \\u1234\\u5678\\u90AB\\uCDEF\"").apply {
            assertString("unicode \u1234\u5678\u90AB\uCDEF")
        }
        scanFirst("\"unicode \\u{1234}\\u{5678}\\u{90AB}\\u{CDEF}\"").apply {
            assertString("unicode \u1234\u5678\u90AB\uCDEF")
        }
        scanFirst("\"string with unicode escape outside BMP \\u{1F600}\"").apply {
            assertString("string with unicode escape outside BMP 😀")
        }
        scanFirst("\"string with minimal unicode escape \\u{0}\"").apply {
            assertString("string with minimal unicode escape \u0000")
        }
        scanFirst("\"string with maximal unicode escape \\u{10FFFF}\"").apply {
            assertString("string with maximal unicode escape \uDBFF\uDFFF")
        }
        scanFirst("\"string with maximal minimal unicode escape \\u{0000000}\"").apply {
            assertString("string with maximal minimal unicode escape \u0000")
        }
        scanFirst("\"string with unicode surrogate pair escape \\uD83D\\uDE00\"").apply {
            assertString("string with unicode surrogate pair escape 😀")
        }
        scanFirst("\"string with minimal surrogate pair escape \\uD800\\uDC00\"").apply {
            assertString("string with minimal surrogate pair escape 𐀀")
        }
        scanFirst("\"string with maximal surrogate pair escape \\uDBFF\\uDFFF\"").apply {
            assertString("string with maximal surrogate pair escape \uDBFF\uDFFF")
        }
    }

    private fun expectScannerException(string: String, block: ScannerException.() -> Unit) {
        try {
            Scanner(string).let {
                while (it.scan() !is Token.EndOfFile) {
                }
            }
        } catch (e: ScannerException) {
            e.block()
            return
        }
        error("an exception was expected")
    }

    @Test
    fun scanReportsUsefulStringErrors() {
        expectScannerException("\"") {
            assertTrue(message!!.contains("Unterminated string"))
        }

        expectScannerException("\"\"\"") {
            assertTrue(message!!.contains("Unterminated block string"))
        }

        expectScannerException("\"\"\"\"") {
            assertTrue(message!!.contains("Unterminated block string"))
        }

        expectScannerException("\"no end quote") {
            assertTrue(message!!.contains("Unterminated string"))
        }

        expectScannerException("'single quotes'") {
            assertTrue(message!!.contains("Unexpected symbol ''' (0x27)"))
        }

        expectScannerException("\"multi\nline\"") {
            assertTrue(message!!.contains("Unterminated string"))
        }

        expectScannerException("\"multi\rline\"") {
            assertTrue(message!!.contains("Unterminated string"))
        }

        expectScannerException("\"bad \\z esc\"") {
            assertTrue(message!!.contains("Invalid escape character '\\z'"))
        }

        expectScannerException("\"bad \\x esc\"") {
            assertTrue(message!!.contains("Invalid escape character '\\x'"))
        }

        expectScannerException("\"bad \\u1 esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u1 '"))
        }
        expectScannerException("\"bad \\u0XX1 esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u0X'"))
        }
        expectScannerException("\"bad \\uXXXX esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uX'"))
        }
        expectScannerException("\"bad \\uFXXX esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uFX'"))
        }
        expectScannerException("\"bad \\uXXXF esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uX'"))
        }
        expectScannerException("\"bad \\u{} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{}'"))
        }
        expectScannerException("\"bad \\u{FXXX} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{FX'"))
        }
        expectScannerException("\"bad \\u{FFFF esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{FFFF '"))
        }
        expectScannerException("\"bad \\u{FFFF\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{FFFF\"'"))
        }
        expectScannerException("\"too high \\u{110000} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{110000}'"))
        }
        expectScannerException("\"way too high \\u{12345678} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{12345678}'"))
        }

        expectScannerException("\"too long \\u{000000000} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{000000000'"))
        }

        expectScannerException("\"bad surrogate \\uDEAD esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uDEAD'"))
        }

        expectScannerException("\"bad surrogate \\u{DEAD} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{DEAD}'"))
        }
        expectScannerException("\"cannot use braces for surrogate pair \\u{D83D}\\u{DE00} esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\u{D83D}'"))
        }
        expectScannerException("\"bad high surrogate pair \\uDEAD\\uDEAD esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uDEAD'"))
        }
        expectScannerException("\"bad low surrogate pair \\uD800\\uD800 esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uD800\\uD800'"))
        }
        expectScannerException("\"cannot escape half a pair \uD83D\\uDE00 esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uDE00'"))
        }
        expectScannerException("\"cannot escape half a pair \\uD83D\uDE00 esc\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uD83D'"))
        }
        expectScannerException("\"bad \\uD83D\\not an escape\"") {
            assertTrue(message!!.contains("Invalid Unicode escape '\\uD83D'"))
        }
    }

    @Test
    fun scansBlockStrings() {
        scanFirst("\"\"\"\"\"\"").apply {
            assertString("")
        }
        scanFirst("\"\"\"simple\"\"\"").apply {
            assertString("simple")
        }
        scanFirst("\"\"\" white space \"\"\"").apply {
            assertString(" white space ")
        }
        scanFirst("\"\"\"contains \" quote\"\"\"").apply {
            assertString("contains \" quote")
        }
        scanFirst("\"\"\"contains \\\"\"\" triple quote\"\"\"").apply {
            assertString("contains \"\"\" triple quote")
        }
        scanFirst("\"\"\"multi\nline\"\"\"").apply {
            assertString("multi\nline")
        }
        scanFirst("\"\"\"multi\rline\r\nnormalized\"\"\"").apply {
            assertString("multi\nline\nnormalized")
        }
        scanFirst("\"\"\"unescaped \\n\\r\\b\\t\\f\\u1234\"\"\"").apply {
            assertString("unescaped \\n\\r\\b\\t\\f\\u1234")
        }
        scanFirst("\"\"\"unescaped unicode outside BMP \\u{1f600}\"\"\"").apply {
            assertString("unescaped unicode outside BMP \\u{1f600}")
        }
        scanFirst("\"\"\"slashes \\\\ \\/\"\"\"").apply {
            assertString("slashes \\\\ \\/")
        }
        scanFirst(
            """""${'"'}

        spans
          multiple
            lines

        ""${'"'}"""
        ).apply {
            assertString("spans\n  multiple\n    lines")
        }
    }

    @Test
    fun advanceLineAfterScanningMultilineBlockString() {
        scanSecond(
            """""${'"'}

        spans
          multiple
            lines

        ""${'"'} second_token"""
        ).apply {
            assertName("second_token")
        }
    }

    @Test
    fun scanReportsUsefulBlockStringErrors() {
        expectScannerException("\"\"\"") {
            assertTrue(message!!.contains("Unterminated block string at 3"))
        }
        expectScannerException("\"\"\"no end quote") {
            assertTrue(message!!.contains("Unterminated block string at 15"))
        }
    }

    @Test
    fun scansNumbers() {
        scanFirst("4").apply {
            assertIs<Token.Int>(this)
            assertEquals(4, value)
        }
        scanFirst("4.123").apply {
            assertIs<Token.Float>(this)
            assertEquals(4.123, value)
        }
        scanFirst("-4").apply {
            assertIs<Token.Int>(this)
            assertEquals(-4, value)
        }
        scanFirst("9").apply {
            assertIs<Token.Int>(this)
            assertEquals(9, value)
        }
        scanFirst("0").apply {
            assertIs<Token.Int>(this)
            assertEquals(0, value)
        }
        scanFirst("-4.123").apply {
            assertIs<Token.Float>(this)
            assertEquals(-4.123, value)
        }
        scanFirst("0.123").apply {
            assertIs<Token.Float>(this)
            assertEquals(0.123, value)
        }
        scanFirst("123e4").apply {
            assertIs<Token.Float>(this)
            assertEquals(123e4, value)
        }
        scanFirst("123E4").apply {
            assertIs<Token.Float>(this)
            assertEquals(123E4, value)
        }
        scanFirst("123e-4").apply {
            assertIs<Token.Float>(this)
            assertEquals(123e-4, value)
        }
        scanFirst("123e+4").apply {
            assertIs<Token.Float>(this)
            assertEquals(123e+4, value)
        }
        scanFirst("-1.123e4").apply {
            assertIs<Token.Float>(this)
            assertEquals(-1.123e4, value)
        }
        scanFirst("-1.123E4").apply {
            assertIs<Token.Float>(this)
            assertEquals(-1.123E4, value)
        }

        scanFirst("-1.123e-4").apply {
            assertIs<Token.Float>(this)
            assertEquals(-1.123e-4, value)
        }

        scanFirst("-1.123e+4").apply {
            assertIs<Token.Float>(this)
            assertEquals(-1.123e+4, value)
        }
        scanFirst("-1.123e4567").apply {
            assertIs<Token.Float>(this)
            assertEquals(Double.NEGATIVE_INFINITY, value)
        }
    }

    @Test
    fun scanReportsUsefulNumberErrors() {
        expectScannerException("00") {
            assertTrue(message!!.contains("Invalid number, unexpected digit after 0: '0'"))
        }
        expectScannerException("01") {
            assertTrue(message!!.contains("Invalid number, unexpected digit after 0: '1'"))
        }
        expectScannerException("01.23") {
            assertTrue(message!!.contains("Invalid number, unexpected digit after 0: '1'"))
        }
        expectScannerException("+1") {
            assertTrue(message!!.contains("Unexpected symbol '+' (0x2b)"))
        }
        expectScannerException("1.") {
            assertTrue(message!!.contains("Unterminated number"))
        }
        expectScannerException("1e") {
            assertTrue(message!!.contains("Unterminated number"))
        }
        expectScannerException("1E") {
            assertTrue(message!!.contains("Unterminated number"))
        }
        expectScannerException("1.e1") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'e'"))
        }
        expectScannerException(".123") {
            assertTrue(message!!.contains("Unterminated spread operator"))
        }
        expectScannerException("1.A") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'A'"))
        }
        expectScannerException("-A") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'A'"))
        }
        expectScannerException("1.0e") {
            assertTrue(message!!.contains("Unterminated number"))
        }
        expectScannerException("1.0eA") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'A'"))
        }
        expectScannerException("1.0e\"") {
            assertTrue(message!!.contains("Invalid number, expected digit but got '\"'"))
        }
        expectScannerException("1.2e3e") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'e'"))
        }
        expectScannerException("1.2e3.4") {
            assertTrue(message!!.contains("Invalid number, expected digit but got '.'"))
        }
        expectScannerException("1.23.4") {
            assertTrue(message!!.contains("Invalid number, expected digit but got '.'"))
        }
        expectScannerException("0xF1") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'x'"))
        }
        expectScannerException("0b10") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'b'"))
        }
        expectScannerException("123abc") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'a'"))
        }
        expectScannerException("1_234") {
            assertTrue(message!!.contains("Invalid number, expected digit but got '_'"))
        }
        expectScannerException("1\u00DF") {
            assertTrue(message!!.contains("Unexpected symbol 'ß' (0xdf)"))
        }
        expectScannerException("1.23f") {
            assertTrue(message!!.contains("Invalid number, expected digit but got 'f'"))
        }
        expectScannerException("1.234_5") {
            assertTrue(message!!.contains("Invalid number, expected digit but got '_'"))
        }
    }
}
