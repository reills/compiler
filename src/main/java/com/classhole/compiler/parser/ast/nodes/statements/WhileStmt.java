package com.classhole.compiler.parser.ast.nodes.statements;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;

public record WhileStmt(Exp condition, Stmt body) implements Stmt {}