package com.classhole.compiler.parser.ast;

import com.classhole.compiler.parser.ast.nodes.definitions.ClassDef;
import java.util.List;

public record Program(List<ClassDef> classes, List<Stmt> entryPoint) {}