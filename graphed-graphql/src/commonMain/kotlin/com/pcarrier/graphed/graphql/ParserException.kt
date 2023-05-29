package com.pcarrier.graphed.graphql

class ParserException(message: String, token: Token) : Exception("$message at ${token.start}:${token.end} ($token)")
