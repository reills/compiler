package com.classhole.compiler.typechecker;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.definitions.*;
import com.classhole.compiler.parser.ast.nodes.statements.*;

import com.classhole.compiler.parser.ast.nodes.expressions.IntLiteralExp;
import com.classhole.compiler.parser.ast.nodes.expressions.PrintlnExp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TypeCheckerTest {

  @Test
  public void testSimpleProgramPassesTypeCheck() {
    // Create a program like:
    // var x: Int;
    // println(1);

    VarDecStmt varStmt = new VarDecStmt("Int", "x");
    Exp printlnExp = new PrintlnExp(new IntLiteralExp(1));
    PrintStmt printStmt = new PrintStmt(printlnExp);

    Program program = new Program(
        List.of(), // No classes
        List.of(varStmt, printStmt)
    );

    TypeChecker checker = new TypeChecker();

    // Should not throw any exceptions
    assertDoesNotThrow(() -> checker.check(program));
  }

  @Test
  public void testUninitializedVarThrows() {
    // var x: Int;
    // println(x); // should error (x not initialized)

    VarDecStmt varStmt = new VarDecStmt("Int", "x");
    Exp printlnExp = new PrintlnExp(new com.classhole.compiler.parser.ast.nodes.expressions.VarExp("x"));
    PrintStmt printStmt = new PrintStmt(printlnExp);

    Program program = new Program(
        List.of(),
        List.of(varStmt, printStmt)
    );

    TypeChecker checker = new TypeChecker();

    try {
      checker.check(program);
    } catch (RuntimeException ex) {
      assert(ex.getMessage().contains("initialization"));
      return;
    }

    throw new AssertionError("Expected exception for uninitialized variable");
  }
}
