package com.pcarrier.graphed.graphql

import kotlin.math.pow

class Scanner(val src: String) {
    private val len = src.length
    private var pos = 0

    fun reset() {
        pos = 0
    }

    fun remaining(): List<Token> =
        buildList {
            while (true) {
                val token = scan()
                add(token)
                if (token is Token.EndOfFile) return@buildList
            }
        }

    fun scan(): Token {
        skipWhitespaceAndComments()
        if (pos == len) {
            return Token.EndOfFile(pos)
        }
        return when (val c = src[pos]) {
            in ('A'..'Z'), '_', in ('a'..'z') -> scanName()
            '-', in '0'..'9' -> scanNumber()
            '"' -> {
                if (len > pos + 2 && src[pos + 1] == '"' && src[pos + 2] == '"') {
                    return scanBlockString()
                } else {
                    return scanString()
                }
            }

            '!' -> return Token.ExclamationPoint(pos).also { pos++ }
            '$' -> return Token.Dollar(pos).also { pos++ }
            '&' -> return Token.Ampersand(pos).also { pos++ }
            '(' -> return Token.LeftParenthesis(pos).also { pos++ }
            ')' -> return Token.RightParenthesis(pos).also { pos++ }
            '.' -> {
                if (len > pos + 2 && src[pos + 1] == '.' && src[pos + 2] == '.') {
                    return Token.Spread(pos).also { pos += 3 }
                } else {
                    throw ScannerException("Unfinished spread operator", pos)
                }
            }

            ':' -> return Token.Colon(pos).also { pos++ }
            '=' -> return Token.Equals(pos).also { pos++ }
            '@' -> return Token.At(pos).also { pos++ }
            '[' -> return Token.LeftBracket(pos).also { pos++ }
            ']' -> return Token.RightBracket(pos).also { pos++ }
            '{' -> return Token.LeftBrace(pos).also { pos++ }
            '}' -> return Token.RightBrace(pos).also { pos++ }
            '|' -> return Token.Pipe(pos).also { pos++ }
            else -> throw ScannerException("Unexpected symbol '${c}'", pos)
        }
    }

    private fun scanUnicode(): Char {
        val start = pos
        if (++pos == len) {
            throw ScannerException("Unfinished Unicode escape", start)
        }
        val codeString = when (src[pos]) {
            '{' -> {
                val builder = StringBuilder()
                var seen = 0;
                while (true) {
                    if (++pos == len) {
                        throw ScannerException("Unfinished Unicode escape", start)
                    }
                    val c = src[pos]
                    if (c == '}') {
                        break
                    } else if (c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f') {
                        seen++
                        if (seen > 4) {
                            throw ScannerException("Invalid Unicode escape, too many characters", start)
                        }
                        builder.append(c)
                    } else {
                        throw ScannerException("Invalid Unicode escape, unexpected character", start)
                    }
                }
                builder.toString().also { pos++ }
            }

            in '0'..'9', in 'A'..'F', in 'a'..'f' -> {
                if (pos + 3 >= len) {
                    throw ScannerException("Unfinished Unicode escape", start)
                }
                src.substring(pos, pos + 4).also { pos += 4 }
            }

            else -> throw ScannerException("Invalid Unicode escape, unexpected character", start)
        }
        return try {
            codeString.toInt(16).toChar()
        } catch (_: NumberFormatException) {
            throw ScannerException("Invalid Unicode escape", start)
        }
    }

    private fun scanString(): Token {
        val builder = StringBuilder()
        val start = pos
        pos++

        while (pos < len) {
            when (val c = src[pos]) {
                '"' -> return Token.String(start, ++pos, builder.toString())

                '\\' -> {
                    if (pos == len - 1) {
                        throw ScannerException("Unfinished escaping", pos)
                    }
                    when (val e = src[++pos]) {
                        'b' -> builder.append('\b').also { pos++ }
                        'f' -> builder.append('\u000C').also { pos++ }
                        'n' -> builder.append('\n').also { pos++ }
                        'r' -> builder.append('\r').also { pos++ }
                        't' -> builder.append('\t').also { pos++ }
                        'u' -> builder.append(scanUnicode())
                        else -> builder.append(e).also { pos++ }
                    }
                }

                '\n' -> throw ScannerException("Newline in non-block string", pos)
                else -> builder.append(c).also { pos++ }
            }
        }
        throw ScannerException("Unfinished string", pos)
    }

    private fun scanBlockStringForIndent(): Int {
        var p = pos
        var indent = Int.MAX_VALUE
        var lindent = 0
        var leftWs = false
        while (p < len) {
            when (src[p]) {
                '"' -> {
                    if (p + 2 < len && src[p + 1] == '"' && src[p + 2] == '"') {
                        return indent
                    }
                }

                '\\' -> {
                    if (p + 3 < len && src[p + 1] == '\"' && src[p + 2] == '\"' && src[p + 3] == '\"') {
                        p += 3
                    }
                }

                '\n' -> break
                else -> {}
            }
            p++
        }
        while (p < len) {
            when (src[p]) {
                '"' -> {
                    if (p + 2 < len && src[p + 1] == '"' && src[p + 2] == '"') {
                        if (leftWs && lindent < indent) indent = lindent
                        return indent
                    }
                    leftWs = true
                }

                '\\' -> {
                    leftWs = true
                    if (p + 3 < len && src[p + 1] == '\"' && src[p + 2] == '\"' && src[p + 3] == '\"') {
                        p += 3
                    }
                }

                ' ', '\t' -> if (!leftWs) lindent++
                '\n' -> {
                    if (leftWs && lindent < indent) indent = lindent
                    lindent = 0
                    leftWs = true
                }

                else -> leftWs = true
            }
            p++
        }
        return indent
    }

    private fun scanBlockString(): Token {
        val start = pos
        pos += 3
        val indent = scanBlockStringForIndent()
        val builder = StringBuilder()
        var lstart = pos
        var inIndent = true
        var seenNonWs = false
        var ws = 0
        var lproduced = 0
        while (pos < len) {
            when (val c = src[pos]) {
                ' ', '\t' -> {
                    ws++
                    if (inIndent) {
                        if (ws == indent) {
                            inIndent = false
                        }
                    } else {
                        builder.append(c).also { lproduced++ }
                    }
                }

                '"' -> {
                    inIndent = false
                    seenNonWs = true
                    if (pos + 2 < len && src[pos + 1] == '"' && src[pos + 2] == '"') {
                        if (ws == pos - lstart) {
                            if (seenNonWs) {
                                builder.setLength(builder.length - lproduced)
                            } else {
                                builder.setLength(0)
                            }
                        }
                        pos += 3
                        for (i in builder.length - 1 downTo 0) {
                            if (builder[i] != '\n') {
                                builder.setLength(i + 1)
                                break
                            }
                        }
                        return Token.String(start, pos, builder.toString())
                    } else {
                        builder.append(c).also { lproduced++ }
                    }
                }

                '\\' -> {
                    inIndent = false
                    seenNonWs = true
                    when (val e = src[++pos]) {
                        'b' -> builder.append('\b').also { lproduced++ }
                        'f' -> builder.append('\u000C').also { lproduced++ }
                        'n' -> builder.append('\n').also { lproduced++ }
                        'r' -> builder.append('\r').also { lproduced++ }
                        't' -> builder.append('\t').also { lproduced++ }
                        'u' -> builder.append(scanUnicode()).also { lproduced++ }
                        '"' -> {
                            if (pos + 2 < len && src[pos + 1] == '"' && src[pos + 2] == '"') {
                                builder.append("\"\"\"").also { pos += 2; lproduced += 3 }
                            } else {
                                builder.append(e).also { lproduced++ }
                            }
                        }

                        else -> builder.append(e).also { lproduced++ }
                    }
                }
                '\r' -> {
                    if (pos + 1 == len || src[pos + 1] != '\n') {
                        builder.append(c).also { lproduced++ }
                    }
                }
                '\n' -> {
                    if (ws == pos - lstart) {
                        if (seenNonWs) {
                            builder.setLength(builder.length - lproduced)
                            builder.append(c)
                        } else {
                            builder.setLength(0)
                        }
                    } else {
                        builder.append(c)
                    }
                    inIndent = true
                    ws = 0
                    lstart = pos + 1
                    lproduced = 0
                }

                else -> {
                    inIndent = false
                    seenNonWs = true
                    builder.append(c).also { lproduced++ }
                }
            }
            pos++
        }
        throw ScannerException("Unfinished block string", start)
    }

    private fun scanNumber(): Token {
        val start = pos
        var float = false
        var period = -1
        var negnum = false
        var num = 0L
        var ne = false
        var e = 0
        if (src[pos] == '-') {
            negnum = true
            pos++
        }

        while (pos < len) {
            when (val c = src[pos]) {
                in '0'..'9' -> {
                    num = (num * 10 + (c - '0'))
                    if (period >= 0) period++
                    pos++
                }

                '.' -> {
                    if (float) {
                        throw ScannerException("Unexpected period", pos)
                    }
                    float = true
                    period = 0
                    pos++
                }

                'e', 'E' -> {
                    float = true
                    pos++
                    when (src[pos]) {
                        '+' -> pos++
                        '-' -> {
                            ne = true
                            pos++
                        }
                    }
                    while (pos < len) {
                        val ec = src[pos]
                        when (ec) {
                            in '0'..'9' -> {
                                e = e * 10 + (ec - '0')
                                pos++
                            }

                            else -> break
                        }
                    }
                }

                '_', in 'A'..'Z', in 'a'..'z' ->
                    throw ScannerException("A number cannot be followed by '${c}'", pos)

                else -> break
            }
        }

        if (negnum) num = -num
        if (float) {
            var res = num.toDouble()
            if (ne) e = -e
            if (period >= 0) e -= period
            return Token.Float(start, pos, res * 10.toDouble().pow(e))
        } else {
            return Token.Int(start, pos, num)
        }
    }

    private fun scanName(): Token {
        val start = pos
        val n = StringBuilder()
        while (pos < len) {
            val c = src[pos]
            when (c) {
                '_', in '0'..'9', in 'A'..'Z', in 'a'..'z' -> {
                    n.append(c)
                    pos++
                }

                else -> break
            }
        }
        return Token.Name(start = start, end = pos, value = n.toString())
    }

    private fun skipWhitespaceAndComments() {
        while (pos < len) {
            val code: Char = src[pos]
            // tab | NL | CR | space | comma | BOM
            if (code == '\t' ||
                code == '\n' ||
                code == '\r' ||
                code == ' ' ||
                code == ',' ||
                code == 0xfeff.toChar()
            ) {
                pos++
            } else if (code == '#') {
                while (pos < len && src[pos] != '\n') {
                    pos++
                }
            } else {
                break
            }
        }
    }
}
