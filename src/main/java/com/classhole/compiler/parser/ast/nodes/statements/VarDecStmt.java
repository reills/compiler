package com.classhole.compiler.parser.ast.nodes.statements;

import com.classhole.compiler.parser.ast.Stmt;

public record VarDecStmt(String type, String name) implements Stmt {}

