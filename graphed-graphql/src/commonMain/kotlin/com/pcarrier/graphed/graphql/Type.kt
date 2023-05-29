package com.pcarrier.graphed.graphql

sealed interface Type
class NamedType(val name: String) : Type {
    override fun toString() = name
}

class ListType(val type: Type) : Type {
    override fun toString() = "[$type]"
}
class NonNullType(val type: Type) : Type {
    override fun toString() = "$type!"
}
