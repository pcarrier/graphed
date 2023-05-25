package com.pcarrier.graphed.graphql

sealed class Selection(val directives: List<Directive>)

class FragmentSpread(val name: String, directives: List<Directive>) :
    Selection(directives)

class InlineFragment(val typeCondition: String, directives: List<Directive>, val selections: List<Selection>) :
    Selection(directives)

class Field(
    val alias: String?,
    val name: String,
    val arguments: List<Argument>,
    directives: List<Directive>,
    val selections: List<Selection>
) :
    Selection(directives)
