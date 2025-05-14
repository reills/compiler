package com.classhole.compiler.parser.ast.nodes.statements;


import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;
import java.util.Optional;

public record IfStmt(Exp condition, Stmt thenStmt, Optional<Stmt> elseStmt) implements Stmt {}
