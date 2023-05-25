package com.pcarrier.graphed

import kotlin.math.min
import kotlin.math.pow

class Scanner(val src: String) {
    private val len = src.length
    private var pos = 0
    fun remaining(): List<Token> =
        buildList {
            while (true) {
                val token = next()
                add(token)
                if (token is Token.EndOfFile) return@buildList
            }
        }

    fun next(): Token {
        skipWhitespaceAndComments()
        if (pos == len) {
            return Token.EndOfFile(pos)
        }
        return when (val c = src[pos]) {
            in ('A'..'Z'), '_', in ('a'..'z') -> readName()
            '-', in '0'..'9' -> readNumber()
            '"' -> {
                if (len > pos + 2 && src[pos + 1] == '"' && src[pos + 2] == '"') {
                    return readBlockString()
                } else {
                    return readString()
                }
            }

            '!' -> return Token.ExclamationPoint(pos).also { pos++ }
            '?' -> return Token.QuestionMark(pos).also { pos++ }
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

    private fun readUnicode(): Char {
        val start = pos
        if (++pos == len) {
            throw ScannerException("Unfinished Unicode escape", start)
        }
        val codeString = when (src[pos]) {
            '{' -> {
                val builder = StringBuilder()
                while (true) {
                    if (++pos == len) {
                        throw ScannerException("Unfinished Unicode escape", start)
                    }
                    val c = src[pos]
                    if (c == '}') {
                        break
                    } else if (c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f') {
                        builder.append(c)
                    } else {
                        throw ScannerException("Invalid Unicode escape", start)
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

            else -> throw ScannerException("Invalid Unicode escape", start)
        }
        return try {
            codeString.toInt(16).toChar()
        } catch (_: NumberFormatException) {
            throw ScannerException("Invalid Unicode escape", start)
        }
    }

    private fun readString(): Token {
        val builder = StringBuilder()
        val start = pos
        pos++

        while (pos < len) {
            when (val c = src[pos]) {
                '"' -> return Token.String(start, ++pos, builder.toString())

                '\\' -> {
                    if (pos == len) {
                        throw ScannerException("Unfinished escaping", pos)
                    }
                    when (val e = src[++pos]) {
                        'b' -> builder.append('\b').also { pos++ }
                        'f' -> builder.append('\u000C').also { pos++ }
                        'n' -> builder.append('\n').also { pos++ }
                        'r' -> builder.append('\r').also { pos++ }
                        't' -> builder.append('\t').also { pos++ }
                        'u' -> builder.append(readUnicode())
                        else -> builder.append(e).also { pos++ }
                    }
                }

                '\n' -> throw ScannerException("Newline in non-block string", pos)
                else -> builder.append(c).also { pos++ }
            }
        }
        throw ScannerException("Unfinished string", pos)
    }

    private fun readBlockString(): Token {
        val start = pos
        pos += 3
        val lines = mutableListOf<String>()
        var builder = StringBuilder()

        while (pos < len) {
            when (val c = src[pos]) {
                '"' -> {
                    if (pos + 2 < len && src[pos + 1] == '"' && src[pos + 2] == '"') {
                        pos += 3
                        lines.add(builder.toString())
                        var firstNonEmpty: Int? = null
                        var lastNonEmpty = -1
                        var commonIndent = Int.MAX_VALUE
                        lines.forEachIndexed { i, s ->
                            val indent = countLeadingWhitespace(s)
                            if (indent != null) {
                                if (firstNonEmpty == null) firstNonEmpty = i
                                lastNonEmpty = i
                                if (i != 0 && indent < commonIndent) commonIndent = indent
                            }
                        }
                        return Token.String(start, pos,
                            lines
                                .mapIndexed { i, l -> if (i == 0) l else l.substring(min(l.length, commonIndent)) }
                                .subList(firstNonEmpty ?: 0, lastNonEmpty + 1)
                                .joinToString("\n")
                        ).also { pos++ }
                    } else {
                        builder.append(c).also { pos++ }
                    }
                }

                '\\' -> {
                    if (pos == len) {
                        throw ScannerException("Unfinished escaping", pos)
                    }
                    when (val e = src[++pos]) {
                        '"' -> {
                            if (pos + 2 < len && src[pos + 1] == '"' && src[pos + 2] == '"') {
                                builder.append("\"\"\"").also { pos += 3 }
                            } else {
                                builder.append("\"").also { pos++ }
                            }
                        }

                        'b' -> builder.append('\b').also { pos++ }
                        'f' -> builder.append('\u000C').also { pos++ }
                        'n' -> builder.append('\n').also { pos++ }
                        'r' -> builder.append('\r').also { pos++ }
                        't' -> builder.append('\t').also { pos++ }
                        'u' -> builder.append(readUnicode())
                        else -> builder.append(e).also { pos++ }
                    }
                }

                '\n' -> {
                    lines.add(builder.toString())
                    builder = StringBuilder()
                    pos++
                }

                else -> builder.append(c).also { pos++ }
            }
        }
        throw ScannerException("Unfinished string", pos)
    }

    private fun countLeadingWhitespace(src: String): Int? {
        for (idx in src.indices) {
            if (src[idx] != ' ' && src[idx] != '\t') {
                return idx
            }
        }
        return null
    }

    private fun readNumber(): Token {
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

        if (float) {
            if (negnum) num = -num
            var res = num.toDouble()
            if (ne) e = -e
            if (period >= 0) e -= period
            return Token.Float(start, pos, res * 10.toDouble().pow(e))
        } else {
            if (negnum) num = -num
            return Token.Int(start, pos, num)
        }
    }

    private fun readName(): Token {
        val start = pos
        val n = StringBuilder()
        while (pos < len) {
            val c = src[pos]
            if (c == '_' || c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z') {
                n.append(c)
                pos++
            } else {
                break
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
