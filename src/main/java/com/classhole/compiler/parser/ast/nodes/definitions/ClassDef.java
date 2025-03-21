package com.classhole.compiler.parser.ast.nodes.definitions;

import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import java.util.List;

public record ClassDef(
    String className,
    String parentClass, // nullable
    List<VarDecStmt> fields,
    ConstructorDef constructor,
    List<MethodDef> methods
) {}