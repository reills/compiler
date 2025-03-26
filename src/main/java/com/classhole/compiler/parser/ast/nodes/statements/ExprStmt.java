package com.classhole.compiler.parser.ast.nodes.statements;

import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.Exp;

public record ExprStmt(Exp exp) implements Stmt {}
