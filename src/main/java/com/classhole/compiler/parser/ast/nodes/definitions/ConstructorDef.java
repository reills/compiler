package com.classhole.compiler.parser.ast.nodes.definitions;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import java.util.List;
import java.util.Optional;

public record ConstructorDef(
    List<VarDecStmt> parameters,
    Optional<List<Exp>> superArgs,
    List<Stmt> body
) {}
