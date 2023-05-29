package com.pcarrier.graphed.graphql

sealed interface Value
sealed interface DynValue : Value
sealed interface ConstValue : Value
sealed interface ListValue : Value {
    val values: List<Value>
}

sealed interface ObjectValue : Value {
    val values: Map<String, Value>
}

class BooleanValue(val value: Boolean) : ConstValue, DynValue
class ConstListValue(override val values: List<ConstValue>) : ConstValue, ListValue
class ConstObjectValue(override val values: Map<String, ConstValue>) : ConstValue, ObjectValue
class DynListValue(override val values: List<DynValue>) : DynValue, ListValue
class DynObjectValue(override val values: Map<String, DynValue>) : DynValue, ObjectValue
class EnumValue(val value: String) : ConstValue, DynValue
class FloatValue(val value: Double) : ConstValue, DynValue
class IntValue(val value: Long) : ConstValue, DynValue
class StringValue(val value: String) : ConstValue, DynValue
class VariableValue(val name: String) : DynValue
object NullValue : ConstValue, DynValue
