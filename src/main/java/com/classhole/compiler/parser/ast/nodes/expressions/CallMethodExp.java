package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;
import java.util.List;

public record CallMethodExp(Exp target, String methodName, List<Exp> args) implements Exp {}
