package com.pcarrier.graphed

sealed class Definition(val directives: List<Directive>)
sealed class ExecutableDefinition(directives: List<Directive>, val selections: List<Selection>) :
    Definition(directives)

class OperationDefinition(
    val name: String?,
    val variables: List<VariableDefinition>,
    directives: List<Directive>,
    val type: OperationType,
    selections: List<Selection>
) :
    ExecutableDefinition(directives, selections)

class FragmentDefinition(
    val name: String,
    val typeCondition: String,
    directives: List<Directive>,
    selections: List<Selection>
) :
    ExecutableDefinition(directives, selections)

sealed class TypeSystemDefinition(val description: String?, val extend: Boolean, directives: List<Directive>) :
    Definition(directives)

class SchemaDefinition(
    description: String?,
    extend: Boolean,
    directives: List<Directive>,
    val operationTypes: Map<OperationType, String>
) :
    TypeSystemDefinition(description, extend, directives)

class DirectiveDefinition(
    description: String?,
    val name: String,
    val arguments: List<InputValueDefinition>,
    val repeatable: Boolean,
    val locations: List<DirectiveLocation>
) :
    TypeSystemDefinition(description, false, emptyList())

sealed class TypeDefinition(
    description: String?,
    extend: Boolean,
    val name: String,
    directives: List<Directive>
) :
    TypeSystemDefinition(description, extend, directives)

class ScalarTypeDefinition(
    description: String?,
    extend: Boolean,
    name: String,
    directives: List<Directive>
) :
    TypeDefinition(description, extend, name, directives)

class FieldDefinition(
    val description: String?,
    val name: String,
    val arguments: List<InputValueDefinition>,
    val type: Type,
    directives: List<Directive>
) :
    Definition(directives)

class ObjectTypeDefinition(
    description: String?,
    extend: Boolean,
    name: String,
    directives: List<Directive>,
    val interfaces: List<String>,
    val fields: List<FieldDefinition>,
) :
    TypeDefinition(description, extend, name, directives)

class InterfaceTypeDefinition(
    description: String?,
    extend: Boolean,
    name: String,
    directives: List<Directive>,
    val interfaces: List<String>,
    val fields: List<FieldDefinition>,
) :
    TypeDefinition(description, extend, name, directives)

class UnionTypeDefinition(
    description: String?,
    extend: Boolean,
    name: String,
    directives: List<Directive>,
    val types: List<String>
) :
    TypeDefinition(description, extend, name, directives)

class EnumTypeDefinition(
    description: String?,
    extend: Boolean,
    name: String,
    directives: List<Directive>,
    val values: List<EnumValueDefinition>
) :
    TypeDefinition(description, extend, name, directives)

class EnumValueDefinition(val description: String?, val name: String, directives: List<Directive>) :
    Definition(directives)

class InputObjectTypeDefinition(
    description: String?,
    extend: Boolean,
    name: String,
    directives: List<Directive>,
    val fields: List<InputValueDefinition>
) :
    TypeDefinition(description, extend, name, directives)

class InputValueDefinition(
    val description: String?,
    val name: String,
    val type: Type,
    val defaultValue: Value?,
    directives: List<Directive>
) :
    Definition(directives)

class VariableDefinition(
    val name: String,
    val type: Type,
    val defaultValue: Value?,
    directives: List<Directive>
) :
    Definition(directives)
