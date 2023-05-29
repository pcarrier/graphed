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

class Printer {
    val builder = StringBuilder()
    var last: Token = EndOfFile

    fun reset() {
        builder.setLength(0)
        last = EndOfFile
    }

    fun printDocument(doc: Document): String {
        reset()
        doc.definitions.forEach { printDefinition(it) }
        return builder.toString()
    }

    fun printToken(tok: Token) {
        when (tok) {
            is Token.Ampersand -> builder.append("&")
            is Token.At -> builder.append("@")
            is Token.Colon -> builder.append(":")
            is Token.Dollar -> builder.append("$")
            is Token.EndOfFile -> {}
            is Token.Equals -> builder.append("=")
            is Token.ExclamationPoint -> builder.append("!")
            is Token.Float -> builder.append(tok.value)
            is Token.Int -> builder.append(tok.value)
            is Token.LeftBrace -> builder.append("{")
            is Token.LeftBracket -> builder.append("[")
            is Token.LeftParenthesis -> builder.append("(")
            is Token.Name -> {
                if (last is Token.Name || last is Token.Int || last is Token.Float) builder.append(" ")
                builder.append(tok.value)
            }

            is Token.Pipe -> builder.append("|")
            is Token.RightBrace -> builder.append("}")
            is Token.RightBracket -> builder.append("]")
            is Token.RightParenthesis -> builder.append(")")
            is Token.Spread -> builder.append("...")
            is Token.String -> {
                builder.append("\"")
                builder.append(tok.value.replace(escaped) { escapeSequences[it.value[0].code] })
                builder.append("\"")
            }
        }
        last = tok
    }

    private fun printDefinition(def: TopLevelDefinition) {
        when (def) {
            is FragmentDefinition -> {
                printToken(Fragment)
                printToken(name(def.name))
                printToken(On)
                printToken(name(def.typeCondition))
                printDirectives(def.directives)
                printSelections(def.selections)
            }

            is OperationDefinition -> {
                printToken(name(def.type.repr))
                if (def.name != null) {
                    printToken(name(def.name))
                }
                printVariableDefinitions(def.variables)
                printDirectives(def.directives)
                printSelections(def.selections)
            }

            is DirectiveDefinition -> {
                if (def.description != null) printToken(string(def.description))
                printToken(Directive)
                printToken(At)
                printToken(name(def.name))
                printParenInputValueDefinitions(def.args)
                if (def.locations.isNotEmpty()) {
                    printToken(On)
                    def.locations.forEachIndexed { i, loc ->
                        if (i > 0) printToken(Pipe)
                        printToken(name(loc.name))
                    }
                }
            }

            is SchemaDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Schema)
                printDirectives(def.directives)
                printOperationTypes(def.operationTypes)
            }

            is EnumTypeDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Enum)
                printToken(name(def.name))
                printDirectives(def.directives)
                printEnumValues(def.values)
            }

            is InputObjectTypeDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Input)
                printToken(name(def.name))
                printDirectives(def.directives)
                printBracedInputValueDefinitions(def.fields)
            }

            is InterfaceTypeDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Interface)
                printToken(name(def.name))
                printDirectives(def.directives)
                printFieldDefinitions(def.fields)
            }

            is ObjectTypeDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Type)
                printToken(name(def.name))
                if (def.interfaces.isNotEmpty()) {
                    printToken(Implements)
                    def.interfaces.forEachIndexed { i, iface ->
                        if (i > 0) printToken(Ampersand)
                        printToken(name(iface))
                    }
                }
                printDirectives(def.directives)
                printFieldDefinitions(def.fields)
            }

            is ScalarTypeDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Scalar)
                printToken(name(def.name))
                printDirectives(def.directives)
            }

            is UnionTypeDefinition -> {
                if (def.description != null) printToken(string(def.description))
                if (def.extend) printToken(Extend)
                printToken(Union)
                printToken(name(def.name))
                printDirectives(def.directives)
                if (def.types.isNotEmpty()) {
                    printToken(Equals)
                    def.types.forEachIndexed { i, type ->
                        if (i > 0) printToken(Pipe)
                        printToken(name(type))
                    }
                }
            }
        }
    }

    private fun printFieldDefinitions(fields: List<FieldDefinition>) {
        if (fields.isEmpty()) return
        printToken(LeftBrace)
        fields.forEach { printFieldDefinition(it) }
        printToken(RightBrace)
    }

    private fun printFieldDefinition(def: FieldDefinition) {
        if (def.description != null) printToken(string(def.description))
        printToken(name(def.name))
        printParenInputValueDefinitions(def.args)
        printToken(Colon)
        printType(def.type)
        printDirectives(def.directives)
    }

    private fun printEnumValues(values: List<EnumValueDefinition>) {
        if (values.isEmpty()) return
        printToken(LeftBrace)
        values.forEach { printEnumValue(it) }
        printToken(RightBrace)
    }

    private fun printEnumValue(def: EnumValueDefinition) {
        if (def.description != null) printToken(string(def.description))
        printToken(name(def.name))
        printDirectives(def.directives)
    }

    private fun printOperationTypes(operationTypes: Map<OperationType, String>) {
        if (operationTypes.isEmpty()) return
        printToken(LeftBrace)
        operationTypes.forEach { (op, type) ->
            printToken(name(op.name))
            printToken(Colon)
            printToken(name(type))
        }
        printToken(RightBrace)
    }

    private fun printBracedInputValueDefinitions(args: List<InputValueDefinition>) {
        if (args.isEmpty()) return
        printToken(LeftBrace)
        args.forEach { printInputValueDefinition(it) }
        printToken(RightBrace)
    }

    private fun printParenInputValueDefinitions(args: List<InputValueDefinition>) {
        if (args.isEmpty()) return
        printToken(LeftParenthesis)
        args.forEach { printInputValueDefinition(it) }
        printToken(RightParenthesis)
    }

    private fun printInputValueDefinition(def: InputValueDefinition) {
        printToken(name(def.name))
        printToken(Colon)
        printType(def.type)
        if (def.defaultValue != null) {
            printToken(Equals)
            printValue(def.defaultValue)
        }
    }

    private fun printVariableDefinitions(variables: List<VariableDefinition>) {
        if (variables.isEmpty()) return
        printToken(LeftParenthesis)
        variables.forEach { printVariableDefinition(it) }
        printToken(RightParenthesis)
    }

    private fun printVariableDefinition(definition: VariableDefinition) {
        printToken(Dollar)
        printToken(name(definition.name))
        printToken(Colon)
        printType(definition.type)
        if (definition.defaultValue != null) {
            printToken(Equals)
            printValue(definition.defaultValue)
        }
    }

    private fun printType(type: Type) {
        when (type) {
            is ListType -> {
                printToken(LeftBracket)
                printType(type.type)
                printToken(RightBracket)
            }

            is NamedType -> printToken(name(type.name))
            is NonNullType -> {
                printType(type.type)
                printToken(ExclamationPoint)
            }
        }
    }

    private fun printSelections(selections: List<Selection>) {
        if (selections.isEmpty()) return
        printToken(LeftBrace)
        selections.forEach { printSelection(it) }
        printToken(RightBrace)
    }

    private fun printSelection(selection: Selection) {
        when (selection) {
            is Field -> {
                printToken(name(selection.name))
                printArguments(selection.args)
                printDirectives(selection.directives)
                printSelections(selection.selections)
            }

            is FragmentSpread -> {
                printToken(Spread)
                printToken(name(selection.name))
                printDirectives(selection.directives)
            }

            is InlineFragment -> {
                printToken(Spread)
                printToken(On)
                printToken(name(selection.typeCondition))
                printDirectives(selection.directives)
                printSelections(selection.selections)
            }
        }
    }

    private fun printDirectives(directives: List<Directive>) {
        if (directives.isEmpty()) return
        directives.forEach { printDirective(it) }
    }

    private fun printDirective(directive: Directive) {
        printToken(At)
        printToken(name(directive.name))
        printArguments(directive.args)
    }

    private fun printArguments(args: List<Argument>) {
        if (args.isEmpty()) return
        printToken(LeftParenthesis)
        args.forEach { printArgument(it) }
        printToken(RightParenthesis)
    }

    private fun printArgument(arg: Argument) {
        printToken(name(arg.name))
        printToken(Colon)
        printValue(arg.value)
    }

    private fun printValue(value: Value) {
        when (value) {
            is BooleanValue -> if (value.value) printToken(True) else printToken(False)
            is ListValue -> {
                printToken(LeftBracket)
                value.values.forEach { printValue(it) }
                printToken(RightBracket)
            }

            is ObjectValue -> {
                printToken(LeftBrace)
                value.values.forEach {
                    printToken(name(it.key))
                    printToken(Colon)
                    printValue(it.value)
                }
                printToken(RightBrace)
            }

            is EnumValue -> printToken(name(value.value))
            is FloatValue -> printToken(float(value.value))
            is IntValue -> printToken(int(value.value))
            NullValue -> printToken(Null)
            is StringValue -> printToken(string(value.value))
            is VariableValue -> {
                printToken(Dollar)
                printToken(name(value.name))
            }
        }
    }

    companion object {
        val EndOfFile = Token.EndOfFile(-1)
        val ExclamationPoint = Token.ExclamationPoint(-1)
        val Dollar = Token.Dollar(-1)
        val Ampersand = Token.Ampersand(-1)
        val LeftParenthesis = Token.LeftParenthesis(-1)
        val RightParenthesis = Token.RightParenthesis(-1)
        val Spread = Token.Spread(-1)
        val Colon = Token.Colon(-1)
        val Equals = Token.Equals(-1)
        val At = Token.At(-1)
        val LeftBracket = Token.LeftBracket(-1)
        val RightBracket = Token.RightBracket(-1)
        val LeftBrace = Token.LeftBrace(-1)
        val RightBrace = Token.RightBrace(-1)
        val Pipe = Token.Pipe(-1)
        val Extend = name("extend")
        val Schema = name("schema")
        val Fragment = name("fragment")
        val On = name("on")
        val True = name("true")
        val False = name("false")
        val Null = name("null")
        val Directive = name("directive")
        val Input = name("input")
        val Interface = name("interface")
        val Type = name("type")
        val Implements = name("implements")
        val Scalar = name("scalar")
        val Union = name("union")
        val Enum = name("enum")

        private fun name(value: String) = Token.Name(-1, -1, value)
        private fun float(value: Double) = Token.Float(-1, -1, value)
        private fun int(value: Long) = Token.Int(-1, -1, value)
        private fun string(value: String) = Token.String(-1, -1, value)
    }
}
