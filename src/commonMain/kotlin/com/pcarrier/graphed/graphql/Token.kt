package com.pcarrier.graphed.graphql

sealed class Token(val start: kotlin.Int, val end: kotlin.Int) {
    class EndOfFile(pos: kotlin.Int) : Token(pos, pos) {
        override fun toString() = "EOF"
    }

    class ExclamationPoint(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "!"
    }

    class QuestionMark(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "?"
    }

    class Dollar(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "$"
    }

    class Ampersand(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "&"
    }

    class LeftParenthesis(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "("
    }

    class RightParenthesis(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = ")"
    }

    class Spread(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "..."
    }

    class Colon(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = ":"
    }

    class Equals(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "="
    }

    class At(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "@"
    }

    class LeftBracket(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "["
    }

    class RightBracket(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "]"
    }

    class LeftBrace(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "{"
    }

    class RightBrace(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "}"
    }

    class Pipe(pos: kotlin.Int) : Token(pos, pos + 1) {
        override fun toString() = "|"
    }

    class Name(start: kotlin.Int, end: kotlin.Int, val value: kotlin.String) : Token(start, end) {
        override fun toString() = "name{$value}"
    }

    class Int(start: kotlin.Int, end: kotlin.Int, val value: Long) : Token(start, end) {
        override fun toString() = "int{$value}"
    }

    class Float(start: kotlin.Int, end: kotlin.Int, val value: Double) : Token(start, end) {
        override fun toString() = "float{$value}"
    }

    class String(start: kotlin.Int, end: kotlin.Int, val value: kotlin.String) : Token(start, end) {
        override fun toString() = "string{$value}"
    }
}
