package com.pcarrier.graphed.graphql

sealed interface Sexp
data class SexpList(val list: List<Sexp>) : Sexp {
    override fun toString() = list.joinToString(" ", "(", ")")
}

data class SexpAtom(val atom: String) : Sexp {
    override fun toString() = atom
}

data class SexpString(val string: String) : Sexp {
    override fun toString() = Printer.str(string)
}

data class SexpLong(val long: Long) : Sexp {
    override fun toString() = long.toString()
}

data class SexpFloat(val float: Double) : Sexp {
    override fun toString() = float.toString()
}

data class SexpBoolean(val boolean: Boolean) : Sexp {
    override fun toString() = boolean.toString()
}

object SexpNil : Sexp {
    override fun toString() = "nil"
}

data class SexpVariable(val variable: String) : Sexp {
    override fun toString() = "$$variable"
}

object ToSexp {
    fun document(doc: Document): Sexp =
        SexpList(
            listOf(
                SexpAtom("doc"),
                SexpList(doc.definitions.map(ToSexp::definition))
            )
        )

    private fun definition(definition: Definition): Sexp =
        when (definition) {
            is EnumValueDefinition -> SexpList(
                listOf(
                    SexpAtom("enum-value"),
                    SexpAtom(definition.name),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpList(definition.directives.map(ToSexp::directive)),
                )
            )

            is FragmentDefinition -> SexpList(
                listOf(
                    SexpAtom("frag"),
                    SexpAtom(definition.name),
                    SexpString(definition.typeCondition),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.selections.map(ToSexp::selection)),
                )
            )

            is OperationDefinition -> SexpList(
                listOf(
                    SexpAtom("op"),
                    definition.name?.let(::SexpString) ?: SexpNil,
                    SexpList(definition.variables.map(ToSexp::variableDefinition)),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpAtom(definition.type.name),
                    SexpList(definition.selections.map(ToSexp::selection)),
                )
            )

            is FieldDefinition -> SexpList(
                listOf(
                    SexpAtom("field-definition"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.arguments.map(ToSexp::definition)),
                    type(definition.type),
                    SexpList(definition.directives.map(ToSexp::directive)),
                )
            )

            is InputValueDefinition -> SexpList(
                listOf(
                    SexpAtom("input-value"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    type(definition.type),
                    definition.defaultValue?.let(ToSexp::value) ?: SexpNil,
                    SexpList(definition.directives.map(ToSexp::directive)),
                )
            )

            is DirectiveDefinition -> SexpList(
                listOf(
                    SexpAtom("directive"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.arguments.map(ToSexp::definition)),
                    SexpBoolean(definition.repeatable),
                    SexpList(definition.locations.map(ToSexp::directiveLocation)),
                )
            )

            is SchemaDefinition -> SexpList(
                listOf(
                    SexpAtom("schema"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.operationTypes.map(ToSexp::operationType)),
                )
            )

            is EnumTypeDefinition -> SexpList(
                listOf(
                    SexpAtom("enum"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.values.map(ToSexp::definition)),
                )
            )
            is InputObjectTypeDefinition -> SexpList(
                listOf(
                    SexpAtom("input"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.fields.map(ToSexp::definition)),
                )
            )
            is InterfaceTypeDefinition -> SexpList(
                listOf(
                    SexpAtom("interface"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.fields.map(ToSexp::definition)),
                )
            )
            is ObjectTypeDefinition -> SexpList(
                listOf(
                    SexpAtom("type"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.interfaces.map { SexpAtom(it) }),
                    SexpList(definition.fields.map(ToSexp::definition)),
                )
            )
            is ScalarTypeDefinition -> SexpList(
                listOf(
                    SexpAtom("scalar"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.directives.map(ToSexp::directive)),
                )
            )
            is UnionTypeDefinition -> SexpList(
                listOf(
                    SexpAtom("union"),
                    definition.description?.let { SexpString(it) } ?: SexpNil,
                    SexpAtom(definition.name),
                    SexpList(definition.directives.map(ToSexp::directive)),
                    SexpList(definition.types.map { SexpAtom(it) }),
                )
            )
            is VariableDefinition -> SexpList(
                listOf(
                    SexpAtom("var"),
                    SexpAtom(definition.name),
                    type(definition.type),
                    definition.defaultValue?.let(ToSexp::value) ?: SexpNil,
                    SexpList(definition.directives.map(ToSexp::directive)),
                )
            )
        }

    private fun operationType(entry: Map.Entry<OperationType, String>) =
        SexpList(listOf(SexpAtom(entry.key.name), SexpAtom(entry.value)))

    private fun directiveLocation(directiveLocation: DirectiveLocation) = SexpAtom(directiveLocation.name)

    private fun variableDefinition(definition: VariableDefinition) = SexpList(
        listOf(
            SexpAtom("var"),
            SexpAtom(definition.name),
            type(definition.type),
            definition.defaultValue?.let(ToSexp::value) ?: SexpNil,
            SexpList(definition.directives.map(ToSexp::directive)),
        )
    )

    private fun type(type: Type): Sexp = when (type) {
        is NamedType -> SexpAtom(type.name)
        is ListType -> SexpList(listOf(SexpAtom("list"), type(type.type)))
        is NonNullType -> SexpList(listOf(SexpAtom("non-null"), type(type.type)))
    }

    private fun selection(selection: Selection): Sexp = when (selection) {
        is Field -> SexpList(
            listOf(
                SexpAtom("field"),
                selection.alias?.let(::SexpString) ?: SexpNil,
                SexpAtom(selection.name),
                SexpList(selection.arguments.map(ToSexp::argument)),
                SexpList(selection.directives.map(ToSexp::directive)),
                SexpList(selection.selections.map(ToSexp::selection)),
            )
        )

        is FragmentSpread -> SexpList(
            listOf(
                SexpAtom("frag-spread"),
                SexpAtom(selection.name),
                SexpList(selection.directives.map(ToSexp::directive)),
            )
        )

        is InlineFragment -> SexpList(
            listOf(
                SexpAtom("inline-frag"),
                SexpString(selection.typeCondition),
                SexpList(selection.directives.map(ToSexp::directive)),
                SexpList(selection.selections.map(ToSexp::selection)),
            )
        )
    }

    private fun directive(directive: Directive) =
        SexpList(listOf(SexpAtom("directive"), SexpString(directive.name), SexpList(directive.args.map(ToSexp::argument))))

    private fun argument(argument: Argument) =
        SexpList(listOf(SexpAtom("arg"), SexpString(argument.name), value(argument.value)))

    private fun value(value: Value): Sexp = when (value) {
        NullValue -> SexpAtom("null")
        is BooleanValue -> SexpBoolean(value.value)
        is ConstListValue -> SexpList(listOf(SexpAtom("const-list"), SexpList(value.values.map(ToSexp::value))))
        is ConstObjectValue -> SexpList(listOf(SexpAtom("const-obj"), SexpList(value.values.map(ToSexp::objEntry))))
        is EnumValue -> SexpList(listOf(SexpAtom("enum-value"), SexpAtom(value.value)))
        is FloatValue -> SexpFloat(value.value)
        is IntValue -> SexpLong(value.value)
        is StringValue -> SexpString(value.value)
        is DynListValue -> SexpList(listOf(SexpAtom("list"), SexpList(value.values.map(ToSexp::value))))
        is DynObjectValue -> SexpList(listOf(SexpAtom("obj"), SexpList(value.values.map(ToSexp::objEntry))))
        is VariableValue -> SexpVariable(value.name)
    }

    private fun objEntry(entry: Map.Entry<String, Value>) =
        SexpList(listOf(SexpAtom(entry.key), value(entry.value)))
}
