package com.classhole.compiler.codegenerator;

import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.definitions.MethodDef;
import com.classhole.compiler.parser.ast.nodes.definitions.ConstructorDef;
import com.classhole.compiler.parser.ast.nodes.statements.AssignStmt;
import com.classhole.compiler.parser.ast.nodes.statements.BlockStmt;
import com.classhole.compiler.parser.ast.nodes.statements.BreakStmt;
import com.classhole.compiler.parser.ast.nodes.statements.ExprStmt;
import com.classhole.compiler.parser.ast.nodes.statements.IfStmt;
import com.classhole.compiler.parser.ast.nodes.statements.PrintStmt;
import com.classhole.compiler.parser.ast.nodes.statements.ReturnStmt;
import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import com.classhole.compiler.parser.ast.nodes.statements.WhileStmt;
import java.util.List;
import java.util.stream.Collectors;

public class StatementGenerator {
  private final ExpressionGenerator expressionGenerator;

  public StatementGenerator(ExpressionGenerator expressionGenerator) {
    this.expressionGenerator = expressionGenerator;
  }

  public String generateStmt(Stmt stmt) {
    if (stmt instanceof VarDecStmt v) return generateVarDec(v);
    if (stmt instanceof AssignStmt a) return generateAssign(a);
    if (stmt instanceof IfStmt i) return generateIf(i);
    if (stmt instanceof WhileStmt w) return generateWhile(w);
    if (stmt instanceof ReturnStmt r) return generateReturn(r);
    if (stmt instanceof BreakStmt b) return "break;";
    if (stmt instanceof BlockStmt b) return generateBlock(b);
    if (stmt instanceof PrintStmt p) return generatePrint(p);
    if (stmt instanceof ExprStmt e) return generateExprStmt(e);

    throw new RuntimeException("Unknown statement type: " + stmt.getClass());
  }

  private String generateVarDec(VarDecStmt stmt) {
    return "let " + stmt.name() + ";";
  }

  private String generateAssign(AssignStmt stmt) {
    String target = stmt.variableName();
    String value = expressionGenerator.generateExp(stmt.expression());

    // naive heuristic: add this. unless RHS already uses `this.` (or is local var/param)
    if (!target.startsWith("this.")) {
      target = "this." + target;
    }

    return target + " = " + value + ";";
  }


  private String generateIf(IfStmt stmt) {
    String cond = expressionGenerator.generateExp(stmt.condition());
    String thenBranch = generateStmt(stmt.thenStmt());

    StringBuilder sb = new StringBuilder();
    sb.append("if (").append(cond).append(") ");
    sb.append(thenBranch);

    stmt.elseStmt().ifPresent(elseBranch ->
        sb.append(" else ").append(generateStmt(elseBranch))
    );

    return sb.toString();
  }

  private String generateWhile(WhileStmt stmt) {
    String cond = expressionGenerator.generateExp(stmt.condition());
    String body = generateStmt(stmt.body());
    return "while (" + cond + ") " + body;
  }


  private String generateReturn(ReturnStmt stmt) {
    return stmt.expression()
        .map(expr -> "return " + expressionGenerator.generateExp(expr) + ";")
        .orElse("return;");
  }

  private String generateBlock(BlockStmt stmt) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    for (Stmt s : stmt.statements()) {
      sb.append(generateStmt(s)).append("\n");
    }
    sb.append("}");
    return sb.toString();
  }

  private String generateExprStmt(ExprStmt stmt) {
    String code = expressionGenerator.generateExp(stmt.exp());
    return code + ";";
  }

  private String generatePrint(PrintStmt stmt) {
    String arg = expressionGenerator.generateExp(stmt.expression());
    return "console.log(" + arg + ");";
  }


  public String generateConstructor(ConstructorDef constructor, String className, String parentClass, List<VarDecStmt> fields)
  {
  StringBuilder sb = new StringBuilder();

    for (VarDecStmt field : fields) {
      boolean isInConstructorParams = constructor.parameters().stream()
          .anyMatch(param -> param.name().equals(field.name()));
      if (!isInConstructorParams) {
        String defaultValue = getDefaultValue(field.type());
        sb.append("  this.").append(field.name()).append(" = ").append(defaultValue).append(";\n");
      }
    }


    if (constructor.superArgs().isPresent()) {
      String args = constructor.superArgs().get().stream()
          .map(expressionGenerator::generateExp)
          .collect(Collectors.joining(", "));
      sb.append("  ").append(parentClass).append(".call(this")
          .append(args.isEmpty() ? "" : ", " + args)
          .append(");\n");
    }


    for (Stmt stmt : constructor.body()) {
      sb.append("  ").append(generateStmt(stmt)).append("\n");
    }

    return sb.toString();
  }


  private String getDefaultValue(String type) {
    return switch (type) {
      case "Int" -> "0";
      case "Boolean" -> "false";
      case "String" -> "\"\"";
      default -> "null";
    };
  }

  public String generateMethod(String className, MethodDef method) {
    StringBuilder sb = new StringBuilder();
    sb.append(className).append(".prototype.").append(method.name()).append(" = function(");
    sb.append(
        method.parameters().stream()
            .map(VarDecStmt::name)
            .collect(Collectors.joining(", "))
    );

    sb.append(") {\n");
    for (Stmt stmt : method.body()) {
      sb.append("  ").append(generateStmt(stmt)).append("\n");
    }
    sb.append("};\n");
    return sb.toString();
  }
}