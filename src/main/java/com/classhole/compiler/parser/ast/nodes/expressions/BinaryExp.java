package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;

public record BinaryExp(Exp left, String operator, Exp right) implements Exp {}
