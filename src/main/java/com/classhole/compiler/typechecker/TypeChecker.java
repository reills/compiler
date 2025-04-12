package com.classhole.compiler.typechecker;

import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.Exp;

import com.classhole.compiler.parser.ast.nodes.definitions.*;
import com.classhole.compiler.parser.ast.nodes.expressions.*;
import com.classhole.compiler.parser.ast.nodes.statements.*;

import com.classhole.compiler.typechecker.types.PrimitiveType;
import com.classhole.compiler.typechecker.types.BuiltInType;
import com.classhole.compiler.typechecker.types.ClassType;
import com.classhole.compiler.typechecker.Type;

public class TypeChecker {
  private final ClassTable classTable = new ClassTable();
  private final Subtyping subtyping = new Subtyping();

  public void check(Program program) {
    // Phase 1: Build class table and subtype graph
    for (ClassDef classDef : program.classes()) {
      classTable.addClass(classDef);
      classDef.superClass().ifPresent(superName ->
          subtyping.addSubtype(classDef.className(), superName));
    }

    // Phase 2: Type check entry-point statements
    TypeEnvironment globalEnv = new TypeEnvironment();
    for (Stmt stmt : program.entryPoint()) {
      checkStmt(stmt, globalEnv);
    }
  }


  private Type resolveType(String typeName) {
    return switch (typeName) {
      case "Int" -> PrimitiveType.INT;
      case "Boolean" -> PrimitiveType.BOOLEAN;
      case "Void" -> PrimitiveType.VOID;
      case "String" -> BuiltInType.STRING;
      case "Object" -> BuiltInType.OBJECT;
      default -> new ClassType(typeName); // User-defined class
    };
  }


  private void checkStmt(Stmt stmt, TypeEnvironment env) {
    switch (stmt) {
      case VarDecStmt varDec -> env.declare(varDec.name(), resolveType(varDec.type()));
      case AssignStmt assign -> {
        // Placeholder: assume everything is fine
        String varName = assign.variableName();
        TypeEnvironment.VarInfo info = env.lookup(varName);
        if (info == null) {
          throw new RuntimeException("Undeclared variable: " + varName);
        }
        env.initialize(varName);
        checkExp(assign.expression(), env); // Type check the expression anyway
      }
      case BlockStmt block -> {
        TypeEnvironment child = new TypeEnvironment(env);
        for (Stmt s : block.statements()) {
          checkStmt(s, child);
        }
      }
      case ExprStmt exprStmt -> checkExp(exprStmt.exp(), env);
      case PrintStmt print -> checkExp(print.expression(), env);
      case ReturnStmt ret -> {
        if (ret.expression().isPresent()) {
          checkExp(ret.expression().get(), env);
        }
      }
      case IfStmt ifStmt -> {
        checkExp(ifStmt.condition(), env);
        checkStmt(ifStmt.thenStmt(), env);
        ifStmt.elseStmt().ifPresent(e -> checkStmt(e, env));
      }
      case WhileStmt whileStmt -> {
        checkExp(whileStmt.condition(), env);
        checkStmt(whileStmt.body(), env);
      }
      case BreakStmt breakStmt -> {
        // nothing to check
      }
      case SuperStmt superStmt -> {
        // handled only during constructor validation
      }
      case null, default ->
          throw new RuntimeException("Unhandled statement type: " + stmt.getClass());
    }
  }

  private Type checkExp(Exp exp, TypeEnvironment env) {
    return switch (exp) {
      case IntLiteralExp ignored -> PrimitiveType.INT;
      case BooleanLiteralExp ignored -> PrimitiveType.BOOLEAN;
      case StringLiteralExp ignored -> BuiltInType.STRING;
      case VarExp varExp -> {
        TypeEnvironment.VarInfo info = env.lookup(varExp.name());
        if (info == null) {
          throw new RuntimeException("Undeclared variable: " + varExp.name());
        }
        if (!info.isInitialized()) {
          throw new RuntimeException("Variable used before initialization: " + varExp.name());
        }
        yield info.type();
      }

      case ParenExp paren -> checkExp(paren.expression(), env);
      case ThisExp ignored -> throw new RuntimeException("Cannot resolve `this` outside of method context");
      case CallMethodExp call -> throw new UnsupportedOperationException("Method call type resolution not implemented yet");
      case NewObjectExp newObj -> new ClassType(newObj.className());
      case BinaryExp binary -> throw new UnsupportedOperationException("Binary expression type checking not implemented yet");

      case PrintlnExp printIn -> {
        checkExp(printIn.exp(), env);
        yield PrimitiveType.VOID;
      }
      default -> throw new RuntimeException("Unhandled expression: " + exp.getClass());
    };
  }

}
