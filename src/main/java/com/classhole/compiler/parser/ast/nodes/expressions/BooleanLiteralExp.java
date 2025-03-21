package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;

public record BooleanLiteralExp(boolean value) implements Exp {}
