package com.classhole.compiler.parser.ast.nodes.definitions;

import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import java.util.List;

public record MethodDef(
    String name,
    List<VarDecStmt> parameters,
    String returnType,
    List<Stmt> body
) {}
