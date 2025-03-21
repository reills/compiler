package com.classhole.compiler.parser.ast.nodes.definitions;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import java.util.List;

public record ConstructorDef(
    List<VarDecStmt> parameters,
    List<Exp> superArgs, // nullable if no super call
    List<Stmt> body
) {}