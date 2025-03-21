package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;

public record VarExp(String name) implements Exp {}
