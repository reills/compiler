package com.classhole.compiler.parser.ast.nodes.statements;


import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;

public record IfStmt(Exp condition, Stmt thenStmt, Stmt elseStmt) implements Stmt {}
