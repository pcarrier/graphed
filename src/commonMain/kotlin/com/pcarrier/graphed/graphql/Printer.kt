package com.pcarrier.graphed.graphql

val escaped = Regex("[\\x00-\\x1f\\x22\\x5c\\x7f-\\x9f]")
val escapeSequences = listOf(
    "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007",
    "\\b", "\\t", "\\n", "\\u000B", "\\f", "\\r", "\\u000E", "\\u000F",
    "\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
    "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F",
    "", "", "\\\"", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "\\\\", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "\\u007F",
    "\\u0080", "\\u0081", "\\u0082", "\\u0083", "\\u0084", "\\u0085", "\\u0086", "\\u0087",
    "\\u0088", "\\u0089", "\\u008A", "\\u008B", "\\u008C", "\\u008D", "\\u008E", "\\u008F",
    "\\u0090", "\\u0091", "\\u0092", "\\u0093", "\\u0094", "\\u0095", "\\u0096", "\\u0097",
    "\\u0098", "\\u0099", "\\u009A", "\\u009B", "\\u009C", "\\u009D", "\\u009E", "\\u009F",
)

object Printer {
    fun printDocument(doc: Document): String =
        doc.definitions.joinToString("\n") { def(it) }

    private fun def(def: Definition): String =
        when (def) {
            is FragmentDefinition ->
                "fragment ${def.name} on ${def.typeCondition}${dirs(def.directives)}${sels(def.selections)}"

            is OperationDefinition ->
                "${def.name?.let { "${def.type.repr} ${def.name}" } ?: ""}${vars(def.variables)}${dirs(def.directives)}${
                    sels(def.selections)
                }"

            is DirectiveDefinition ->
                "${str(def.description)}directive @${def.name}${
                    if (def.arguments.isEmpty()) "" else def.arguments.joinToString(" ", "(", ")") { def(it) }
                }${if (def.repeatable) " repeatable" else ""} on ${
                    def.locations.joinToString("|")
                }"

            is ScalarTypeDefinition ->
                "${str(def.description)}${if (def.extend) "extend " else ""}scalar ${def.name}${dirs(def.directives)}"

            is UnionTypeDefinition ->
                "${str(def.description)}${if (def.extend) "extend " else ""}union ${def.name}${dirs(def.directives)}${
                    if (def.types.isEmpty()) "" else def.types.joinToString(" ", "{", "}")
                }"

            is SchemaDefinition ->
                "${str(def.description)}${if (def.extend) "extend " else ""}schema${dirs(def.directives)}${
                    def.operationTypes.map { "${it.key.repr}:${it.value}" }.joinToString(" ", "{", "}")
                }"

            is EnumValueDefinition ->
                "${str(def.description)}${def.name}${dirs(def.directives)}"

            is EnumTypeDefinition ->
                "${str(def.description)}enum ${def.name}${dirs(def.directives)}${
                    if (def.values.isEmpty()) "" else def.values.joinToString(" ", "{", "}") { def(it) }
                }"

            is InputValueDefinition ->
                "${str(def.description)} ${def.name}:${def.type}${maybeDefault(def.defaultValue)}${dirs(def.directives)}"

            is InputObjectTypeDefinition -> {
                "${str(def.description)}input ${def.name}${dirs(def.directives)}${
                    if (def.fields.isEmpty()) "" else def.fields.joinToString(" ", "{", "}") { def(it) }
                }"
            }

            is InterfaceTypeDefinition -> {
                "${str(def.description)}interface ${def.name}${impls(def.interfaces)}${dirs(def.directives)}${
                    if (def.fields.isEmpty()) "" else def.fields.joinToString(" ", "{", "}") { def(it) }
                }"
            }

            is ObjectTypeDefinition -> {
                "${str(def.description)}type ${def.name}${impls(def.interfaces)}${dirs(def.directives)}${
                    if (def.fields.isEmpty()) "" else def.fields.joinToString(" ", "{", "}") { def(it) }
                }"
            }

            is FieldDefinition -> {
                "${str(def.description)} ${def.name}${
                    if (def.arguments.isEmpty()) "" else def.arguments.joinToString(" ", "(", ")") { def(it) }
                }:${def.type}${dirs(def.directives)}"
            }

            is VariableDefinition -> "${def.name}:${def.type}${def.defaultValue?.let { `val`(it) } ?: ""}${dirs(def.directives)}"
        }

    private fun impls(interfaces: List<String>) =
        if (interfaces.isEmpty()) "" else interfaces.joinToString("&", " implements ")

    private fun maybeDefault(value: Value?): String =
        if (value == null) "" else "=${`val`(value)}"

    private fun vars(variables: List<VariableDefinition>): String =
        if (variables.isEmpty()) ""
        else variables.joinToString(" ", "(", ")", transform = Printer::def)

    private fun sels(selections: List<Selection>): String =
        if (selections.isEmpty()) "" else selections.joinToString(" ", "{", "}") { sel(it) }

    private fun sel(selection: Selection): String =
        when (selection) {
            is FragmentSpread -> "...${selection.name}${dirs(selection.directives)}"
            is InlineFragment -> "...on ${selection.typeCondition}${dirs(selection.directives)}${sels(selection.selections)}"
            is Field -> "${selection.alias?.let { "$it:" } ?: ""}${selection.name}${argDefs(selection.arguments)}${
                dirs(selection.directives)
            }${sels(selection.selections)}"
        }

    private fun dirs(directives: List<Directive>): String =
        directives.joinToString("") { "@${it.name}${argDefs(it.args)}" }

    private fun argDefs(args: List<Argument>): String =
        if (args.isEmpty()) "" else args.joinToString(" ", "(", ")") { "${it.name}:${`val`(it.value)}" }

    private fun `val`(value: Value): String =
        when (value) {
            NullValue -> "null"
            is BooleanValue -> value.value.toString()
            is ConstListValue -> value.values.joinToString(" ", "[", "]") { `val`(it) }
            is ConstObjectValue -> value.values.map { "${it.key}:${`val`(it.value)}" }.joinToString(" ", "{", "}")
            is DynListValue -> value.values.joinToString(" ", "[", "]") { `val`(it) }
            is DynObjectValue -> value.values.map { "${it.key}:${`val`(it.value)}" }.joinToString(" ", "{", "}")
            is EnumValue -> value.value
            is FloatValue -> value.value.toString()
            is IntValue -> value.value.toString()
            is StringValue -> str(value.value)
            is VariableValue -> "${'$'}${value.name}"
        }

    internal fun str(str: String?): String =
        str?.replace(escaped) { escapeSequences[it.value[0].code] }?.let { "\"$it\"" } ?: ""
}
