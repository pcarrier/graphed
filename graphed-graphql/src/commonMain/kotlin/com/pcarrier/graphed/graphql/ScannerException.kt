package com.pcarrier.graphed.graphql

class ScannerException(message: String, pos: Int) : Exception("$message at $pos")
