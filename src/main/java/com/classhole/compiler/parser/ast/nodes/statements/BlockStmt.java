package com.classhole.compiler.parser.ast.nodes.statements;

import com.classhole.compiler.parser.ast.Stmt;
import java.util.List;

public record BlockStmt(List<Stmt> statements) implements Stmt {}