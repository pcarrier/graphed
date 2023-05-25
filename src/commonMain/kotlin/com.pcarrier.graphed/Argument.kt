package com.pcarrier.graphed

sealed class Argument(val name: String, val value: Value)
class DynArgument(name: String, value: DynValue) : Argument(name, value)
class ConstArgument(name: String, value: ConstValue) : Argument(name, value)
