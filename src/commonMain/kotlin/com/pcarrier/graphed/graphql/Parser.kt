package com.pcarrier.graphed.graphql

class Parser(val src: String) {
    private val scanner = Scanner(src)
    private var token = scanner.scan()

    private fun advance(): Token = scanner.scan().also { token = it }

    fun parse(): Document =
        Document(src, buildList {
            scanner.reset()
            token = scanner.scan()
            while (token !is Token.EndOfFile) {
                if (token is Token.LeftBrace) {
                    val selections = parseSelections(optional = false)
                    add(OperationDefinition(null, emptyList(), emptyList(), OperationType.QUERY, selections))
                    continue
                }
                val description: String? = token.let {
                    if (it is Token.String) it.value.also { advance() } else null
                }

                val extend = token.let {
                    if (it is Token.Name && it.value == "extend") true.also { advance() } else false
                }

                val t = token
                if (t !is Token.Name) throw ParserException("Expected definition type", t)
                val n = advance()
                when (t.value) {
                    "query", "mutation", "subscription" -> {
                        if (description != null) throw ParserException(
                            "Descriptions are not available on operations",
                            t
                        )
                        if (extend) throw ParserException("extend is not available on operations", t)
                        val name = if (n is Token.Name) n.value.also { advance() } else null
                        val type = try {
                            OperationType.fromString(t.value)
                        } catch (_: IllegalArgumentException) {
                            throw ParserException("Unknown operation type ${t.value}", t)
                        }
                        val vars = parseVariables()
                        val directives = parseDirectives(false)
                        val selections = parseSelections(optional = extend)
                        add(OperationDefinition(name, vars, directives, type, selections))
                    }

                    "fragment" -> {
                        if (description != null) throw ParserException(
                            "Descriptions are not available on fragments",
                            t
                        )
                        if (extend) throw ParserException("extend is not available on fragments", t)
                        if (n !is Token.Name) throw ParserException("Expected fragment name", t)
                        val name = n.value
                        val on = advance()
                        if (on !is Token.Name || on.value != "on") throw ParserException(
                            "Expected 'on' after fragment spread",
                            on
                        )
                        val typeCondition = advance().also { advance() }
                        if (typeCondition !is Token.Name) throw ParserException(
                            "Expected type condition",
                            typeCondition
                        )
                        val directives = parseDirectives(false)
                        val selections = parseSelections(optional = extend)
                        add(FragmentDefinition(name, typeCondition.value, directives, selections))
                    }

                    "directive" -> {
                        if (extend) throw ParserException("extend is not available on directive definitions", t)
                        if (n !is Token.At) throw ParserException("Directive names start with '@'", t)
                        val name = advance()
                        if (name !is Token.Name) throw ParserException("Expected directive name after '@'", name)
                        advance()
                        val args = parseArgumentDefinitions()
                        val repeatable = token.let {
                            if (it is Token.Name && it.value == "repeatable") true.also { advance() } else false
                        }
                        val on = token
                        if (on !is Token.Name || on.value != "on") throw ParserException(
                            "Expected 'on' in directive definition",
                            on
                        )
                        advance()
                        val locations = parseDirectiveLocations()
                        add(DirectiveDefinition(description, name.value, args, repeatable, locations))
                    }

                    "schema" -> {
                        val directives = parseDirectives(true)
                        val operationTypes = buildMap {
                            if (token is Token.LeftBrace) {
                                advance()
                                while (token !is Token.RightBrace) {
                                    val t2 = token
                                    if (t2 !is Token.Name) throw ParserException("Expected operation type", t2)
                                    val op = try {
                                        OperationType.fromString(t2.value)
                                    } catch (_: IllegalArgumentException) {
                                        throw ParserException("Unknown operation type ${t2.value}", t2)
                                    }
                                    advance()
                                    val name = token
                                    if (name !is Token.Name) throw ParserException(
                                        "Expected operation type name",
                                        name
                                    )
                                    this.put(op, name.value)
                                    advance()
                                }
                            } else if (directives.isEmpty())
                                throw ParserException("Expected operation type definitions or directives", token)
                        }
                        add(SchemaDefinition(description, extend, directives, operationTypes))
                    }

                    "scalar" -> {
                        if (n !is Token.Name) throw ParserException("Expected scalar name", t)
                        advance()
                        val directives = parseDirectives(true)
                        add(ScalarTypeDefinition(description, extend, n.value, directives))
                    }

                    "type" -> {
                        if (n !is Token.Name) throw ParserException("Expected type name", t)
                        advance()
                        val directives = parseDirectives(true)
                        val interfaces = parseInterfaces()
                        val fields = parseFields()
                        add(ObjectTypeDefinition(description, extend, n.value, directives, interfaces, fields))
                    }

                    "interface" -> {
                        if (n !is Token.Name) throw ParserException("Expected type name", t)
                        advance()
                        val directives = parseDirectives(true)
                        val interfaces = parseInterfaces()
                        val fields = parseFields()
                        if (extend && directives.isEmpty() && interfaces.isEmpty() && fields.isEmpty())
                            throw ParserException(
                                "Expected interface extension (directives, interfaces, or fields)",
                                token
                            )
                        add(InterfaceTypeDefinition(description, extend, n.value, directives, interfaces, fields))
                    }

                    "union" -> {
                        if (n !is Token.Name) throw ParserException("Expected union name", t)
                        advance()
                        val directives = parseDirectives(true)
                        if (token is Token.Equals) advance() else throw ParserException("Expected '='", token)
                        if (token is Token.Pipe) advance()
                        val types = buildList {
                            do {
                                val t2 = token
                                if (t2 !is Token.Name) throw ParserException("Expected type in union", t2)
                                this.add(t2.value)
                                advance()
                            } while ((token is Token.Pipe).also { if (it) advance() })
                        }
                        add(UnionTypeDefinition(description, extend, n.value, directives, types))
                    }

                    "enum" -> {
                        if (n !is Token.Name) throw ParserException("Expected enum name", t)
                        advance()
                        val directives = parseDirectives(true)
                        val values = buildList {
                            if (token is Token.LeftBrace) {
                                advance()
                                while (token !is Token.RightBrace) {
                                    val t2 = token
                                    val desc = if (t2 is Token.String) t2.value.also { advance() } else null
                                    val n2 = token
                                    if (n2 !is Token.Name) throw ParserException("Expected enum value", t2)
                                    val name = n2.value
                                    advance()
                                    val dir = parseDirectives(true)
                                    this.add(EnumValueDefinition(desc, name, dir))
                                }
                                advance()
                            } else {
                                if (extend && directives.isEmpty())
                                    throw ParserException("Expected enum values or directives", token)
                            }
                        }
                        add(EnumTypeDefinition(description, extend, n.value, directives, values))
                    }

                    "input" -> {
                        if (n !is Token.Name) throw ParserException("Expected input name", t)
                        advance()
                        val directives = parseDirectives(true)
                        val fields = buildList {
                            if (token is Token.LeftBrace) {
                                advance()
                                while (token !is Token.RightBrace) {
                                    val t2 = token
                                    val fieldDescr = if (t2 is Token.String) t2.value.also { advance() } else null
                                    val n2 = token
                                    if (n2 !is Token.Name) throw ParserException("Expected input field name", n2)
                                    advance()
                                    val colon = token
                                    if (colon !is Token.Colon) throw ParserException(
                                        "Expected ':' after an input field name",
                                        colon
                                    )
                                    advance()
                                    val type = parseType()
                                    val defaultValue = if (token is Token.Equals) {
                                        advance()
                                        parseValue(true)
                                    } else null
                                    val dir = parseDirectives(true)
                                    this.add(InputValueDefinition(fieldDescr, n2.value, type, defaultValue, dir))
                                }
                                advance()
                            } else {
                                if (extend && directives.isEmpty())
                                    throw ParserException("Expected input fields or directives", token)
                            }
                        }
                        add(InputObjectTypeDefinition(description, extend, n.value, directives, fields))
                    }

                    else -> throw ParserException("Unknown definition type ${t.value}", t)
                }
            }
        })

    private fun parseArgumentDefinitions() = buildList {
        if (token !is Token.LeftParenthesis) return@buildList
        advance()
        while (token !is Token.RightParenthesis) {
            if (token is Token.EndOfFile) throw ParserException("Expected ')'", token)
            val description: String? = token.let {
                if (it is Token.String) it.value.also { advance() } else null
            }
            val name = token
            if (name !is Token.Name) throw ParserException("Expected argument name", name)
            val colon = advance()
            if (colon !is Token.Colon) throw ParserException("Expected ':'", colon)
            advance()
            val type = parseType()
            val defaultValue = if (token is Token.Equals) {
                advance()
                parseValue(true)
            } else null
            val directives = parseDirectives(true)
            this.add(
                InputValueDefinition(
                    description,
                    name.value,
                    type,
                    defaultValue,
                    directives
                )
            )
        }
        advance()
    }

    private fun parseFields() = buildList {
        if (token is Token.LeftBrace) {
            advance()
            while (token !is Token.RightBrace) {
                val t = token
                val description = if (t is Token.String) t.value.also { advance() } else null
                val n = token
                if (n !is Token.Name) throw ParserException("Expected field name", n)
                advance()
                val args = parseArgumentDefinitions()
                val colon = token
                if (colon !is Token.Colon) throw ParserException("Expected ':' then type", colon)
                advance()
                val type = parseType()
                val dir = parseDirectives(true)
                add(FieldDefinition(description, n.value, args, type, dir))
            }
            advance()
        }
    }

    private fun parseInterfaces() = buildList {
        val t = token
        if (t !is Token.Name || t.value != "implements") return@buildList
        if (token is Token.Ampersand) advance()
        do {
            advance()
            val t2 = token
            if (t2 !is Token.Name) throw ParserException("Expected interface name", t2)
            add(t2.value)
            advance()
        } while (token is Token.Ampersand)
    }

    private fun parseDirectiveLocations(): List<DirectiveLocation> = buildList {
        if (token is Token.Pipe) advance()
        do {
            val t = token
            if (t !is Token.Name) throw ParserException("Expected directive location", t)
            add(
                try {
                    DirectiveLocation.valueOf(t.value)
                } catch (_: Exception) {
                    throw ParserException("Unknown directive location ${t.value}", t)
                }
            )
            advance()
        } while ((token is Token.Pipe).also { if (it) advance() })
    }

    private fun parseDirectives(const: Boolean): List<Directive> =
        buildList {
            while (token is Token.At) {
                val name = advance()
                if (name !is Token.Name) throw ParserException("Expected directive name", name)
                advance()
                val args = parseArguments(const)
                add(Directive(name.value, args))
            }
        }

    private fun parseArguments(const: Boolean): List<Argument> = buildList {
        val t = token
        if (t !is Token.LeftParenthesis) return@buildList
        advance()
        while (token !is Token.RightParenthesis) {
            if (token is Token.EndOfFile) throw ParserException("Expected ')'", t)
            val name = token
            if (name !is Token.Name) throw ParserException("Expected argument name", name)
            val colon = advance()
            if (colon !is Token.Colon) throw ParserException("Expected ':'", colon)
            advance()
            val value = parseValue(const)
            add(
                if (const) ConstArgument(name.value, value as ConstValue)
                else DynArgument(name.value, value as DynValue)
            )
        }
        advance()
    }

    private fun parseValue(const: Boolean): Value =
        when (val t = token) {
            is Token.LeftBracket -> parseList(const)
            is Token.LeftBrace -> parseObject(const)

            is Token.Float -> FloatValue(t.value).also { advance() }
            is Token.Int -> IntValue(t.value).also { advance() }
            is Token.String -> StringValue(t.value).also { advance() }

            is Token.Name -> when (t.value) {
                "true" -> BooleanValue(true).also { advance() }
                "false" -> BooleanValue(false).also { advance() }
                "null" -> NullValue.also { advance() }
                else -> EnumValue(t.value).also { advance() }
            }

            is Token.Dollar -> {
                val n = advance()
                if (n !is Token.Name) throw ParserException("Expected variable name", n)
                if (const) throw ParserException("Variables are not allowed in const values", n)
                VariableValue(n.value).also { advance() }
            }

            else -> throw ParserException("Expected value", t)
        }

    private fun parseObject(const: Boolean): ObjectValue {
        advance()
        val fields = buildMap {
            while (token !is Token.RightBrace) {
                if (token is Token.EndOfFile) throw ParserException("Expected '}'", token)
                val name = token
                if (name !is Token.Name) throw ParserException("Expected field name", name)
                advance()
                val colon = token
                if (colon !is Token.Colon) throw ParserException("Expected ':' after input name", colon)
                advance()
                val value = parseValue(const)
                set(name.value, value)
            }
            advance()
        }
        @Suppress("UNCHECKED_CAST")
        return if (const) ConstObjectValue(fields as Map<String, ConstValue>)
        else DynObjectValue(fields as Map<String, DynValue>)
    }

    private fun parseList(const: Boolean): Value {
        advance()
        val values = buildList {
            while (token !is Token.RightBracket) {
                if (token is Token.EndOfFile) throw ParserException("Expected ']'", token)
                add(parseValue(const))
            }
            advance()
        }
        @Suppress("UNCHECKED_CAST")
        return if (const) ConstListValue(values as List<ConstValue>)
        else DynListValue(values as List<DynValue>)
    }

    private fun parseSelections(optional: Boolean): List<Selection> = buildList {
        val b = token
        if (b !is Token.LeftBrace) {
            if (optional) return@buildList
            else throw ParserException("Expected selections ('{')", b)
        }
        advance()

        while (token !is Token.RightBrace) {
            val t = token
            if (t is Token.EndOfFile) throw ParserException("Expected '}'", token)
            val n = advance()
            when (t) {
                is Token.Spread -> {
                    if (n !is Token.Name) throw ParserException("Expected name or 'on' after fragment spread", n)
                    if (n.value == "on") {
                        val typeCondition = advance()
                        if (typeCondition !is Token.Name) throw ParserException(
                            "Expected type condition",
                            typeCondition
                        )
                        val directives = parseDirectives(false)
                        val selections = parseSelections(optional = false)
                        add(InlineFragment(typeCondition.value, directives, selections))
                    } else {
                        val directives = parseDirectives(false)
                        add(FragmentSpread(n.value, directives))
                    }
                }

                is Token.Name -> {
                    val (alias, name) =
                        if (n is Token.Colon) {
                            val n2 = advance()
                            if (n2 !is Token.Name) throw ParserException("Expected field name after alias", n2)
                            advance()
                            t.value to n2.value
                        } else {
                            null to t.value
                        }
                    val args = parseArguments(false)
                    val directives = parseDirectives(false)
                    val selections = parseSelections(true)
                    add(Field(alias, name, args, directives, selections))
                }

                else -> throw ParserException("Expected selection", t)
            }
        }
        advance()
    }.also {
        if (it.isEmpty()) throw ParserException("Expected at least one selection", token)
    }

    private fun parseVariables(): List<VariableDefinition> = buildList {
        if (token !is Token.LeftParenthesis) return@buildList
        advance()
        while (token !is Token.RightParenthesis) {
            val t = token
            if (t !is Token.Dollar) throw ParserException("Expected variable definition starting with '$'", t)
            val name = advance()
            if (name !is Token.Name) throw ParserException("Expected variable name after '$'", name)
            val colon = advance()
            if (colon !is Token.Colon) throw ParserException("Expected ':' after variable name", colon)
            advance()
            val type = parseType()
            val defaultValue = if (token is Token.Equals) {
                advance()
                parseValue(true)
            } else null
            val directives = parseDirectives(true)
            add(VariableDefinition(name.value, type, defaultValue, directives))
        }
        advance()
    }

    private fun parseType(): Type =
        when (val t = token) {
            is Token.Name -> NamedType(t.value).also { advance() }
            is Token.LeftBracket -> {
                advance()
                val inner = parseType()
                if (token !is Token.RightBracket) throw ParserException("Expected ']'", token)
                ListType(inner).also { advance() }
            }

            else -> throw ParserException("Expected type", t)
        }.let { if (token is Token.ExclamationPoint) NonNullType(it).also { advance() } else it }
}
