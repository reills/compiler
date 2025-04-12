package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;

public record ParenExp(Exp expression) implements Exp {}