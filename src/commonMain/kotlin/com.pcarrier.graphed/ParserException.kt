package com.pcarrier.graphed

class ParserException(message: String, token: Token) : Exception("$message at ${token.start}:${token.end} ($token)")
