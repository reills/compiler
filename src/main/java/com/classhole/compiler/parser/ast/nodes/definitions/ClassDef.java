package com.classhole.compiler.parser.ast.nodes.definitions;

import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import java.util.List;
import java.util.Optional;

public record ClassDef(
    String className,
    Optional<String> superClass,
    List<VarDecStmt> fields,
    ConstructorDef constructor,
    List<MethodDef> methods
) {}