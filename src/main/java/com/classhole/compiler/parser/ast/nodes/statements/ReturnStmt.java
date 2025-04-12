package com.classhole.compiler.parser.ast.nodes.statements;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;
import java.util.Optional;

public record ReturnStmt(Optional<Exp> expression) implements Stmt {}
