package com.pcarrier.graphed.graphql

class Scanner(val src: String) {
    private var pos = 0
    private val len = src.length

    private fun discardComment() {
        while (true) {
            if (pos == len) {
                return // EOF will be caught by the main loop
            }
            val c = src[pos++]

            when (c) {
                '\n' -> {
                    break
                }

                '\r' -> {
                    if (pos < len && src[pos] == '\n') {
                        pos++
                    }
                    break
                }
            }
        }
    }

    private fun Char.isNameStart(): Boolean {
        return when (this) {
            '_',
            in 'A'..'Z',
            in 'a'..'z',
            -> true

            else -> false
        }
    }

    fun scan(): Token {
        while (pos < len) {
            val c = src[pos]

            // do not consume the byte just yet, names and numbers need the first by
            if (c.isNameStart()) {
                return readName()
            }
            if (c.isDigit() || c == '-') {
                return readNumber()
            }

            // everything else can consume the byte
            val start = pos
            pos++

            when (c) {
                // whitespace
                0xfeff.toChar(), // BOM https://www.unicode.org/glossary/#byte_order_mark
                '\t',
                ' ',
                ',',
                -> {
                    continue
                }

                '\n' -> {
                }

                '\r' -> {
                    if (pos < len && src[pos] == '\n') {
                        pos++
                    }
                }

                '#' -> {
                    discardComment()
                }

                '!' -> return Token.ExclamationPoint(pos)
                '$' -> return Token.Dollar(pos)
                '&' -> return Token.Ampersand(pos)
                '(' -> return Token.LeftParenthesis(pos)
                ')' -> return Token.RightParenthesis(pos)
                '.' -> {
                    if (pos + 1 < len && src[pos] == '.' && src[pos + 1] == '.') {
                        pos += 2
                        return Token.Spread(pos)
                    } else {
                        throw ScannerException("Unterminated spread operator", pos)
                    }
                }

                ':' -> return Token.Colon(pos)
                '=' -> return Token.Equals(pos)
                '@' -> return Token.At(pos)
                '[' -> return Token.LeftBracket(pos)
                ']' -> return Token.RightBracket(pos)
                '{' -> return Token.LeftBrace(pos)
                '}' -> return Token.RightBrace(pos)
                '|' -> return Token.Pipe(pos)
                '"' -> {
                    return if (pos + 1 < len && src[pos] == '"' && src[pos + 1] == '"') {
                        pos += 2
                        readBlockString()
                    } else {
                        readString()
                    }
                }

                else -> {
                    throw ScannerException("Unexpected symbol '${c}' (0x${c.code.toString(16)})",pos)
                }
            }
        }

        return Token.EndOfFile(pos)
    }

    // we are just after "\u"
    private fun readUnicodeEscape(): Int {
        if (pos == len) {
            throw ScannerException("Unterminated Unicode escape", pos, )
        }

        when (src[pos]) {
            '{' -> {
                pos++
                return readVariableUnicodeEscape()
            }

            else -> {
                val c1 = readFixedUnicodeEscape()

                if (c1.isUnicodeScalar()) {
                    return c1
                }

                val start = pos - 6

                // GraphQL allows JSON-style surrogate pair escape sequences, but only when
                // a valid pair is formed.

                if (c1.isLeadingSurrogate()) {
                    if (pos + 1 < len
                        && src[pos] == '\\'
                        && src[pos + 1] == 'u') {
                        pos += 2
                        val c2 = readFixedUnicodeEscape()
                        if (c2.isTrailingSurrogate()) {
                            return codePoint(c1, c2)
                        }
                    }
                }

                throw ScannerException("Invalid Unicode escape '${src.substring(start, pos)}'", pos,)
            }
        }
    }


    private fun Int.isLeadingSurrogate(): Boolean {
        return this in 0xd800..0xdbff;
    }

    private fun Int.isTrailingSurrogate(): Boolean {
        return this in 0xdc00..0xdfff;
    }

    private fun Int.isUnicodeScalar(): Boolean {
        return this in 0x0000..0xd7ff || this in 0xe000..0x10ffff
    }

    // we are just after '{'
    private fun readVariableUnicodeEscape(): Int {
        var i = 0
        var result = 0

        // An int32 has 8 hex digits max
        while (i < 9) {
            if (pos == len) {
                throw ScannerException("Unterminated Unicode escape", pos)
            }
            val c = src[pos++]

            if (c == '}') {
                if (i == 0) {
                    val start = pos - i - 4
                    // empty unicode escape?
                    throw ScannerException("Invalid Unicode escape '${src.substring(start, pos)}'", pos)
                }

                if (!result.isUnicodeScalar()) {
                    val start = pos - i - 4
                    throw ScannerException("Invalid Unicode escape '${src.substring(start, pos)}'", pos)
                }

                // Verify that the code point is valid?
                return result
            }

            val h = c.decodeHex()
            if (h == -1) {
                val start = pos - i - 4
                throw ScannerException("Invalid Unicode escape '${src.substring(start, pos)}'", pos)
            }

            result = result.shl(4).or(h)
            i++
        }

        val start = pos - i - 3
        throw ScannerException("Invalid Unicode escape '${src.substring(start, pos)}'", pos)
    }

    private fun Char.decodeHex(): Int {
        return when (this.code) {
            in 0x30..0x39 -> {
                this.code - 0x30
            }

            in 0x41..0x46 -> {
                this.code - 0x37
            }

            in 0x61..0x66 -> {
                this.code - 0x57
            }

            else -> -1
        }
    }

    private fun readFixedUnicodeEscape(): Int {
        if (pos + 4 >= len) {
            throw ScannerException("Unterminated Unicode escape", pos)
        }

        var result = 0
        for (i in 0..3) {
            val h = src[pos++].decodeHex()
            if (h == -1) {
                val start = pos - i - 3
                throw ScannerException("Invalid Unicode escape '${src.substring(start, pos)}'", pos)
            }
            result = result.shl(4).or(h)
        }

        return result
    }

    private fun readEscapeCharacter(): Int {
        if (pos == len) {
            throw ScannerException("Unterminated escape", pos)
        }
        val c = src[pos++]

        return when (c) {
            '"' -> '"'.code
            '\\' -> '\\'.code
            '/' -> '/'.code
            'b' -> '\b'.code
            'f' -> '\u000C'.code
            'n' -> '\n'.code
            'r' -> '\r'.code
            't' -> '\t'.code
            'u' -> readUnicodeEscape()
            else -> throw ScannerException("Invalid escape character '\\${c}'", pos)
        }
    }

    private fun readString(): Token {
        val builder = StringBuilder()
        val start = pos - 1 // because of "

        while (true) {
            if (pos == len) {
                throw ScannerException("Unterminated string", pos)
            }
            val c = src[pos++]

            when (c) {
                '\\' -> builder.appendCodePointMpp(readEscapeCharacter())
                '\"' -> return Token.String(start, pos, builder.toString())
                '\r', '\n' -> throw ScannerException("Unterminated string", pos)
                else -> {
                    // TODO: we are lenient here and allow potentially invalid chars like invalid surrogate pairs
                    builder.append(c)
                }
            }
        }
    }

    private fun readBlockString(): Token {
        val start = pos - 3 // because of """
        val blockLines = mutableListOf<String>()
        val currentLine = StringBuilder()

        while (true) {
            if (pos == len) {
                throw ScannerException("Unterminated block string", pos)
            }
            val c = src[pos++]

            when (c) {
                '\n' -> {
                    blockLines.add(currentLine.toString())
                    currentLine.clear()
                }

                '\r' -> {
                    if (pos + 1 < len && src[pos] == '\n') {
                        pos++
                    }
                    blockLines.add(currentLine.toString())
                    currentLine.clear()
                }

                '\\' -> {
                    if (pos + 2 < len &&
                        src[pos] == '\"' &&
                        src[pos + 1] == '\"' &&
                        src[pos + 2] == '\"'
                    ) {
                        pos += 3
                        currentLine.append("\"\"\"")
                    } else {
                        currentLine.append(c)
                    }
                }

                '\"' -> {
                    if (pos + 1 < len &&
                        src[pos] == '\"' &&
                        src[pos + 1] == '\"'
                    ) {
                        pos += 2

                        blockLines.add(currentLine.toString())

                        return Token.String(
                            start,
                            pos,
                            blockLines.dedentBlockStringLines().joinToString("\n")
                        )
                    } else {
                        currentLine.append(c)
                    }
                }

                else -> {
                    // TODO: we are lenient here and allow potentially invalid chars like invalid surrogate pairs
                    currentLine.append(c)
                }
            }
        }
    }

    private fun Char.isDigit(): Boolean {
        return when (this) {
            in '0'..'9' -> true
            else -> false
        }
    }

    private val STATE_NEGATIVE_SIGN = 1
    private val STATE_ZERO = 2
    private val STATE_DOT_EXP = 3
    private val STATE_INTEGER_DIGIT = 4
    private val STATE_FRACTIONAL_DIGIT = 5
    private val STATE_SIGN = 6
    private val STATE_EXP_DIGIT = 7
    private val STATE_EXP = 8

    private fun readNumber(): Token {
        val start = pos
        var isFloat = false

        var state = STATE_NEGATIVE_SIGN

        while (pos < len) {
            when (state) {
                STATE_NEGATIVE_SIGN -> {
                    when (src[pos]) {
                        '-' -> {
                            pos++
                            state = STATE_ZERO
                        }
                        else -> {
                            state = STATE_ZERO
                        }
                    }
                }
                STATE_ZERO -> {
                    var c = src[pos]
                    when  {
                        c == '0' -> {
                            pos++
                            state = STATE_DOT_EXP

                            if (pos == len) {
                                break
                            }
                            c = src[pos]
                            if (pos < len && c.isDigit()) {
                                throw ScannerException("Invalid number, unexpected digit after 0: '${c}'", pos)
                            }
                        }
                        c.isDigit() -> {
                            pos++
                            state = STATE_INTEGER_DIGIT
                        }
                        else -> {
                            throw ScannerException("Invalid number, expected digit but got '${c}'", pos)
                        }
                    }
                }
                STATE_INTEGER_DIGIT -> {
                    if (src[pos].isDigit()) {
                        pos++
                    } else {
                        state = STATE_DOT_EXP
                    }
                }
                STATE_DOT_EXP -> {
                    when(src[pos]) {
                        '.' -> {
                            isFloat = true
                            pos++

                            if (pos == len) {
                                throw ScannerException("Unterminated number", pos)
                            }
                            val c = src[pos]
                            if (!c.isDigit()) {
                                throw ScannerException("Invalid number, expected digit but got '${c}'", pos)
                            }
                            pos++
                            state = STATE_FRACTIONAL_DIGIT
                        }
                        else -> {
                            state = STATE_EXP
                        }
                    }
                }
                STATE_EXP -> {
                    when(src[pos]) {
                        'e', 'E' -> {
                            isFloat = true
                            pos++
                            if (pos == len) {
                                throw ScannerException("Unterminated number", pos)
                            }
                            state = STATE_SIGN
                        }
                        else -> break
                    }
                }
                STATE_SIGN -> {
                    var c = src[pos]
                    when (c) {
                        '-', '+' -> {
                            pos++
                            if (pos == len) {
                                throw ScannerException("Unterminated number", pos)
                            }
                            c = src[pos]
                            if (!c.isDigit()) {
                                throw ScannerException("Invalid number, expected digit but got '${c}'", pos)
                            }
                            pos++
                            state = STATE_EXP_DIGIT
                        }
                        else -> {
                            if (!c.isDigit()) {
                                throw ScannerException("Invalid number, expected digit but got '${c}'", pos)
                            }
                            pos++
                            state = STATE_EXP_DIGIT
                        }
                    }
                }
                STATE_EXP_DIGIT -> {
                    if (src[pos].isDigit()) {
                        pos++
                    } else {
                        break
                    }
                }
                STATE_FRACTIONAL_DIGIT -> {
                    if (src[pos].isDigit()) {
                        pos++
                    } else {
                        state = STATE_EXP
                    }
                }
            }
        }

        // Numbers cannot be followed by . or NameStart
        if (pos < len && (src[pos] == '.' || src[pos].isNameStart())) {
            throw ScannerException("Invalid number, expected digit but got '${src[pos]}'", pos)
        }

        val asString = src.substring(start, pos)

        return if (isFloat) {
            Token.Float(start, pos, asString.toDouble())
        } else {
            Token.Int(start, pos, asString.toLong())
        }
    }

    private fun Char.isNameContinue(): Boolean {
        return when (this) {
            '_',
            in '0'..'9',
            in 'A'..'Z',
            in 'a'..'z',
            -> true

            else -> false
        }
    }

    private fun readName(): Token {
        val start = pos

        pos++

        while (pos < len) {
            val c = src[pos]
            if (c.isNameContinue()) {
                pos++
            } else {
                break
            }
        }
        return Token.Name(start, pos, value = src.substring(start, pos))
    }

    fun remaining(): List<Token> {
        return buildList {
            while (true) {
                val token = scan()
                add(token)
                if (token is Token.EndOfFile) return@buildList
            }
        }
    }

    fun reset() {
        pos = 0
    }
}

internal fun List<String>.dedentBlockStringLines(): List<String> {
    var commonIndent = Int.MAX_VALUE
    var firstNonEmptyLine: Int? = null
    var lastNonEmptyLine = -1

    for (i in indices) {
        val line = get(i)
        val indent = line.leadingWhitespace()

        if (indent == line.length) {
            continue
        }

        if (firstNonEmptyLine == null) {
            firstNonEmptyLine = i
        }
        lastNonEmptyLine = i

        if (i != 0 && indent < commonIndent) {
            commonIndent = indent
        }
    }

    return mapIndexed { index, line ->
        if (index == 0) {
            line
        } else {
            line.substring(commonIndent.coerceAtMost(line.length))
        }
    }.subList(firstNonEmptyLine ?: 0, lastNonEmptyLine + 1)
}

internal fun String.leadingWhitespace(): Int {
    var i = 0
    while (i < length && get(i).isWhitespace()) {
        i++
    }

    return i
}
