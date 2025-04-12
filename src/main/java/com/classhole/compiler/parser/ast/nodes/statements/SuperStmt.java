package com.classhole.compiler.parser.ast.nodes.statements;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;
import java.util.List;

public record SuperStmt(List<Exp> args) implements Stmt {}
