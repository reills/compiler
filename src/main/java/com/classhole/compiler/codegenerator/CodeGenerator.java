package com.classhole.compiler.codegenerator;

import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.nodes.definitions.ClassDef;
import com.classhole.compiler.parser.ast.nodes.definitions.MethodDef;
import com.classhole.compiler.parser.ast.nodes.definitions.ConstructorDef;

public class CodeGenerator {
  private final ExpressionGenerator exprGen = new ExpressionGenerator();
  private final StatementGenerator stmtGen = new StatementGenerator(exprGen);

  public String generate(Program program) {
    StringBuilder sb = new StringBuilder();

    for (ClassDef cls : program.classes()) {
      sb.append(generateClass(cls)).append("\n");
    }
    for (var stmt : program.entryPoint()) {
      sb.append(stmtGen.generateStmt(stmt)).append("\n");
    }
    return sb.toString();
  }

  public String generateClass(ClassDef cls) {
    StringBuilder sb = new StringBuilder();
    String className = cls.className();  // accessor for a record
    String parent = cls.superClass().orElse(null);

    sb.append("function ").append(className).append("() {\n");
    sb.append(stmtGen.generateConstructor(
        cls.constructor(),
        className,
        cls.superClass().orElse("Object"),  // Default to Object if no superclass
        cls.fields()
    ));


    sb.append("}\n");

    if (parent != null) {
      sb.append(className).append(".prototype = Object.create(")
          .append(parent).append(".prototype);\n");
      sb.append(className).append(".prototype.constructor = ").append(className).append(";\n");
    }

    for (MethodDef method : cls.methods()) {
      sb.append(generateMethod(className, method));
    }

    return sb.toString();
  }

  public String generateMethod(String className, MethodDef method) {
    return stmtGen.generateMethod(className, method);
  }
}
