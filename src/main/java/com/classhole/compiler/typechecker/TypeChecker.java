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
import com.classhole.compiler.typechecker.InitState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeChecker {
  private final ClassTable classTable = new ClassTable();
  private final Subtyping subtyping = new Subtyping();

  private String currentClass = null;
  private String currentMethod = null;
  private String currentReturnType = null;

  public void check(Program program) {
    // Phase 1: Build class table and subtype graph
    for (ClassDef classDef : program.classes()) {
      classTable.addClass(classDef);
      classDef.superClass().ifPresent(superName -> subtyping.addSubtype(classDef.className(), superName));
    }

    // Then check for cycles
    for (ClassDef classDef : program.classes()) {
      if (classDef.superClass().isPresent()) {
        detectCycles(classDef.className(), new HashSet<>());
      }
    }

    // Phase 2: Type check entry-point statements
    TypeEnvironment globalEnv = new TypeEnvironment();
    InitState globalInitState = new InitState();
    for (Stmt stmt : program.entryPoint()) {
      checkStmt(stmt, globalEnv, globalInitState);
    }

    // Phase 2: Type check class methods
    for (ClassDef classDef : program.classes()) {
      currentClass = classDef.className();
      for (MethodDef method : classDef.methods()) {
        currentMethod = method.name();
        currentReturnType = method.returnType();

        TypeEnvironment methodEnv = new TypeEnvironment();
        InitState methodInitState = new InitState();

        // Add 'this' to env
        methodEnv.declare("this", new ClassType(currentClass));
        methodEnv.initialize("this");

        // Add parameters to env
        for (VarDecStmt param : method.parameters()) {
          Type paramType = resolveType(param.type());
          methodEnv.declare(param.name(), paramType);
          methodEnv.initialize(param.name());
          methodInitState.initialize(param.name());
        }

        for (Stmt stmt : method.body()) {
          checkStmt(stmt, methodEnv, methodInitState);
        }

        // check non-void methods have a return on all paths
        if (!currentReturnType.equals("Void") && !mustReturn(method.body())) {
          throw new RuntimeException("Method " + currentMethod +
              " may not return on all code paths (declared return type: " + currentReturnType + ")");
        }
      }

      // Check constructor's super(...) call if present
      ConstructorDef constructor = classDef.constructor();
      currentClass = classDef.className();

      if (constructor.superArgs().isPresent()) {
        List<Exp> args = constructor.superArgs().get();

        // Look up superclass
        if (classDef.superClass().isEmpty()) {
          throw new RuntimeException("Class " + classDef.className() +
              " cannot call super(); it has no superclass");
        }

        String superClassName = classDef.superClass().get();
        ClassTable.ClassInfo superClass = classTable.getClass(superClassName);
        ConstructorDef superConstructor = superClass.constructor;

        List<VarDecStmt> superParams = superConstructor.parameters();

        if (args.size() != superParams.size()) {
          throw new RuntimeException("Constructor super(...) call in class " + currentClass +
              " expects " + superParams.size() + " args but got " + args.size());
        }

        for (int i = 0; i < args.size(); i++) {
          Type argType = checkExp(args.get(i), new TypeEnvironment()); // no locals yet
          Type expected = resolveType(superParams.get(i).type());
          if (!subtyping.isSubtype(argType.getName(), expected.getName())) {
            throw new RuntimeException("super() arg " + i + " in class " + currentClass +
                " has type " + argType + ", expected " + expected);
          }
        }
      }
    }

    for (ClassDef classDef : program.classes()) {
      if (classDef.superClass().isEmpty())
        continue;

      String subclassName = classDef.className();
      String superClassName = classDef.superClass().get();

      ClassTable.ClassInfo superClass = classTable.getClass(superClassName);
      Map<String, MethodDef> superMethods = superClass.methods;

      for (MethodDef method : classDef.methods()) {
        if (!superMethods.containsKey(method.name()))
          continue;

        MethodDef superMethod = superMethods.get(method.name());

        // Check return type
        Type subReturn = resolveType(method.returnType());
        Type superReturn = resolveType(superMethod.returnType());

        if (!subtyping.isSubtype(subReturn.getName(), superReturn.getName())) {
          throw new RuntimeException("Method " + method.name() + " in subclass " + subclassName +
              " has incompatible return type " + subReturn + ", expected " + superReturn);
        }

        // Check parameter types
        List<VarDecStmt> subParams = method.parameters();
        List<VarDecStmt> superParams = superMethod.parameters();

        if (subParams.size() != superParams.size()) {
          throw new RuntimeException("Method " + method.name() + " in subclass " + subclassName +
              " must have same number of parameters as in superclass " + superClassName);
        }

        for (int i = 0; i < subParams.size(); i++) {
          Type subType = resolveType(subParams.get(i).type());
          Type superType = resolveType(superParams.get(i).type());

          if (!subType.getName().equals(superType.getName())) {
            throw new RuntimeException("Method " + method.name() + " in subclass " + subclassName +
                ": parameter " + i + " type " + subType + " does not match " + superType);
          }
        }
      }
    }

  }

  private void detectCycles(String className, Set<String> visited) {
    if (visited.contains(className)) {
      throw new RuntimeException("Cyclic inheritance detected involving class: " + className);
    }

    visited.add(className);

    ClassTable.ClassInfo info = classTable.getClass(className);
      info.superClassName.ifPresent(s -> detectCycles(s, visited));

    // Remove from visited set when backtracking
    visited.remove(className);
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

  // ok -> return;
  // ok -> if (...) return; else return;
  // not ok -> if (...) return;
  // not ok -> while (...) return;
  private boolean mustReturn(List<Stmt> stmts) {
    boolean didReturn = false;
    for (Stmt stmt : stmts) {
      if (mustReturn(stmt)) {
        didReturn = true;
        break;
      }
    }
    return didReturn;
  }

  private boolean mustReturn(Stmt stmt) {
    return switch (stmt) {
      case ReturnStmt ignored -> true;
      case BlockStmt block -> mustReturn(block.statements());
      case IfStmt ifStmt -> {
        if (ifStmt.elseStmt().isEmpty())
          yield false;
        yield mustReturn(ifStmt.thenStmt()) && mustReturn(ifStmt.elseStmt().get());
      }
      case WhileStmt ignored -> false; // can't guarantee it runs
      default -> false;
    };
  }

  private void checkStmt(Stmt stmt, TypeEnvironment env) {
    switch (stmt) {
      case VarDecStmt varDec -> env.declare(varDec.name(), resolveType(varDec.type()));
      case AssignStmt assign -> {
        String varName = assign.variableName();
        TypeEnvironment.VarInfo info = env.lookup(varName);
        if (info == null) {
          throw new RuntimeException("Undeclared variable: " + varName);
        }

        Type expected = info.type();
        Type actual = checkExp(assign.expression(), env);

        if (!subtyping.isSubtype(actual.getName(), expected.getName())) {
          throw new RuntimeException("Cannot assign " + actual + " to variable '" + varName + "' of type " + expected);
        }

        env.initialize(varName);
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
        Type declaredReturnType = resolveType(currentReturnType);

        if (ret.expression().isPresent()) {
          Type actualReturnType = checkExp(ret.expression().get(), env);
          if (!subtyping.isSubtype(actualReturnType.getName(), declaredReturnType.getName())) {
            throw new RuntimeException("Return type mismatch in method " + currentMethod +
                ": expected " + declaredReturnType + ", but got " + actualReturnType);
          }
        } else {
          // return without expression
          if (!declaredReturnType.equals(PrimitiveType.VOID)) {
            throw new RuntimeException(
                "Method " + currentMethod + " must return a value of type " + declaredReturnType);
          }
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
      case null, default ->
        throw new RuntimeException("Unhandled statement type: " +
            (stmt == null ? "null" : stmt.getClass()));
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

      case ThisExp ignored -> {
        if (currentClass == null) {
          throw new RuntimeException("Cannot use `this` outside of a method");
        }
        yield new ClassType(currentClass);
      }

      case CallMethodExp call -> {
        Type receiverType = checkExp(call.target(), env);

        if (!(receiverType instanceof ClassType classType)) {
          throw new RuntimeException("Cannot call method on non-class type: " + receiverType);
        }

        MethodDef method = classTable.getMethod(classType.name(), call.methodName());
        if (method == null) {
          throw new RuntimeException("Method " + call.methodName() + " not found in class " + classType.name());
        }

        // Check argument types
        List<Type> expectedParamTypes = method.parameters().stream()
            .map(param -> resolveType(param.type()))
            .toList();

        if (expectedParamTypes.size() != call.args().size()) {
          throw new RuntimeException("Argument count mismatch for method " + call.methodName());
        }

        for (int i = 0; i < expectedParamTypes.size(); i++) {
          Type argType = checkExp(call.args().get(i), env);
          Type expected = expectedParamTypes.get(i);
          if (!subtyping.isSubtype(argType.getName(), expected.getName())) {
            throw new RuntimeException("Argument " + i + " to method " + call.methodName() +
                " has type " + argType + ", expected " + expected);
          }
        }

        yield resolveType(method.returnType());
      }
      case NewObjectExp newObj -> new ClassType(newObj.className());

      case BinaryExp binary -> {
        Type leftType = checkExp(binary.left(), env);
        Type rightType = checkExp(binary.right(), env);
        String op = binary.operator();

        // Arithmetic ops: +, -, *, /
        final boolean isNotLeftAndRightIntType = !leftType.equals(PrimitiveType.INT)
            || !rightType.equals(PrimitiveType.INT);
        switch (op) {
          case "+", "-", "*", "/" -> {
            if (isNotLeftAndRightIntType) {
              throw new RuntimeException("Arithmetic operator '" + op + "' requires Int operands.");
            }
            yield PrimitiveType.INT;
          }

          // Comparison ops: <, >, <=, >=, ==, !=
          case "<", ">", "<=", ">=" -> {
            if (isNotLeftAndRightIntType) {
              throw new RuntimeException("Comparison operator '" + op + "' requires Int operands.");
            }
            yield PrimitiveType.BOOLEAN;
          }

          // Equality ops: ==, != (allow comparing any types)
          case "==", "!=" -> {
            // optional: allow any type comparison for now
            yield PrimitiveType.BOOLEAN;
          }
        }

        throw new RuntimeException("Unknown binary operator: " + op);
      }

      case PrintlnExp printIn -> {
        checkExp(printIn.exp(), env);
        yield PrimitiveType.VOID;
      }
      default -> throw new RuntimeException("Unhandled expression: " + exp.getClass());
    };
  }

}
