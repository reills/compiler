package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;

public record PrintlnExp(Exp exp) implements Exp {}
