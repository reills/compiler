package com.classhole.compiler.codegenerator;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.nodes.expressions.BinaryExp;
import com.classhole.compiler.parser.ast.nodes.expressions.BooleanLiteralExp;
import com.classhole.compiler.parser.ast.nodes.expressions.CallMethodExp;
import com.classhole.compiler.parser.ast.nodes.expressions.IntLiteralExp;
import com.classhole.compiler.parser.ast.nodes.expressions.NewObjectExp;
import com.classhole.compiler.parser.ast.nodes.expressions.PrintlnExp;
import com.classhole.compiler.parser.ast.nodes.expressions.StringLiteralExp;
import com.classhole.compiler.parser.ast.nodes.expressions.VarExp;
import com.classhole.compiler.parser.ast.nodes.expressions.ParenExp;
import com.classhole.compiler.parser.ast.nodes.expressions.ThisExp;

import java.util.List;

public class ExpressionGenerator {

  public String generateExp(Exp exp) {
    if (exp instanceof VarExp v) return generateVar(v);
    if (exp instanceof IntLiteralExp i) return generateIntLiteral(i);
    if (exp instanceof BooleanLiteralExp b) return generateBooleanLiteral(b);
    if (exp instanceof StringLiteralExp s) return generateStringLiteral(s);
    if (exp instanceof ThisExp t) return "this";
    if (exp instanceof CallMethodExp c) return generateCall(c);
    if (exp instanceof BinaryExp b) return generateBinary(b);
    if (exp instanceof ParenExp p) return "(" + generateExp(p.expression()) + ")";
    if (exp instanceof NewObjectExp n) return generateNew(n);
    if (exp instanceof PrintlnExp p) return generatePrintln(p);


    throw new RuntimeException("Unknown expression type: " + exp.getClass());
  }

  private String generatePrintln(PrintlnExp exp) {
    String arg = generateExp(exp.exp());
    return "console.log(" + arg + ")";
  }

  private String generateVar(VarExp exp) {
    return exp.name();
  }

  private String generateIntLiteral(IntLiteralExp exp) {
    return String.valueOf(exp.value());
  }

  private String generateBooleanLiteral(BooleanLiteralExp exp) {
    return String.valueOf(exp.value());
  }

  private String generateStringLiteral(StringLiteralExp exp) {
    return "\"" + exp.value().replace("\"", "\\\"") + "\"";
  }

  private String generateBinary(BinaryExp exp) {
    String left = generateExp(exp.left());
    String right = generateExp(exp.right());
    String op = exp.operator(); // assumed to be "+", "-", "*", "/", etc.
    return "(" + left + " " + op + " " + right + ")";
  }

  private String generateCall(CallMethodExp exp) {
    String base = generateExp(exp.receiver());
    for (CallMethodExp.CallLink link : exp.chain()) {
      String methodName = link.methodName();
      List<String> args = link.args().stream()
          .map(this::generateExp)
          .toList();
      base = base + "." + methodName + "(" + String.join(", ", args) + ")";
    }
    return base;
  }


  private String generateNew(NewObjectExp exp) {
    String className = exp.className();
    List<String> args = exp.args().stream()
        .map(this::generateExp)
        .toList();
    return "new " + className + "(" + String.join(", ", args) + ")";
  }




  // Additional methods will be added, like:
  // public String generateBinary(BinaryExp exp) { ... }
  // public String generateCall(CallMethodExp exp) { ... }
}