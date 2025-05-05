package com.classhole.compiler.typechecker;

import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.definitions.*;
import com.classhole.compiler.parser.ast.nodes.statements.*;
import com.classhole.compiler.parser.ast.nodes.expressions.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        List.of(varStmt, printStmt));

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
        List.of(varStmt, printStmt));

    TypeChecker checker = new TypeChecker();

    try {
      checker.check(program);
    } catch (RuntimeException ex) {
      assert (ex.getMessage().contains("initialization"));
      return;
    }

    throw new AssertionError("Expected exception for uninitialized variable");
  }

  @Test
  public void testValidAdditionExpression() {
    Exp printlnExp = new PrintlnExp(
        new com.classhole.compiler.parser.ast.nodes.expressions.BinaryExp(
            new IntLiteralExp(1), "+", new IntLiteralExp(2)));
    Program program = new Program(List.of(), List.of(new PrintStmt(printlnExp)));
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testInvalidAdditionTypesThrows() {
    Exp printlnExp = new PrintlnExp(
        new com.classhole.compiler.parser.ast.nodes.expressions.BinaryExp(
            new com.classhole.compiler.parser.ast.nodes.expressions.StringLiteralExp("hi"),
            "+",
            new IntLiteralExp(2)));
    Program program = new Program(List.of(), List.of(new PrintStmt(printlnExp)));
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  @Test
  public void testEqualityOnDifferentTypes() {
    Exp printlnExp = new PrintlnExp(
        new com.classhole.compiler.parser.ast.nodes.expressions.BinaryExp(
            new IntLiteralExp(1),
            "==",
            new com.classhole.compiler.parser.ast.nodes.expressions.StringLiteralExp("s")));
    Program program = new Program(List.of(), List.of(new PrintStmt(printlnExp)));
    assertDoesNotThrow(() -> new TypeChecker().check(program)); // equality allowed across types
  }
  /////////////////////////////////////////////////////////////////

  @Test
  public void testValidMethodCall() {
    MethodDef method = new MethodDef(
        "get",
        List.of(),
        "Int",
        List.of(new ReturnStmt(Optional.of(new IntLiteralExp(42)))));
    ConstructorDef constructor = new ConstructorDef(
        List.of(),
        Optional.empty(),
        List.of());
    ClassDef klass = new ClassDef(
        "A",
        Optional.empty(),
        List.of(),
        constructor,
        List.of(method));

    Exp call = new CallMethodExp(
        new NewObjectExp("A", List.of()),
        "get",
        List.of());
    Program program = new Program(List.of(klass), List.of(new PrintStmt(new PrintlnExp(call))));
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testMethodCallWrongArgCountThrows() {
    MethodDef method = new MethodDef(
        "set",
        List.of(new VarDecStmt("Int", "x")),
        "Void",
        List.of());
    ConstructorDef constructor = new ConstructorDef(
        List.of(),
        Optional.empty(),
        List.of());
    ClassDef klass = new ClassDef(
        "A",
        Optional.empty(),
        List.of(),
        constructor,
        List.of(method));

    Exp call = new CallMethodExp(
        new NewObjectExp("A", List.of()),
        "set",
        List.of() // Missing argument
    );
    Program program = new Program(List.of(klass), List.of(new PrintStmt(new PrintlnExp(call))));
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  @Test
  public void testMethodCallWrongArgTypeThrows() {
    MethodDef method = new MethodDef(
        "set",
        List.of(new VarDecStmt("Int", "x")),
        "Void",
        List.of());
    ConstructorDef constructor = new ConstructorDef(
        List.of(),
        Optional.empty(),
        List.of());
    ClassDef klass = new ClassDef(
        "A",
        Optional.empty(),
        List.of(),
        constructor,
        List.of(method));

    Exp call = new CallMethodExp(
        new NewObjectExp("A", List.of()),
        "set",
        List.of(new StringLiteralExp("wrongType")) // Incorrect type
    );
    Program program = new Program(List.of(klass), List.of(new PrintStmt(new PrintlnExp(call))));
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  /////////////////////////////////////////
  @Test
  public void testThisUsedInsideMethod() {
    MethodDef method = new MethodDef(
        "foo",
        List.of(),
        "Void",
        List.of(new ExprStmt(new ThisExp())));
    ConstructorDef constructor = new ConstructorDef(
        List.of(),
        Optional.empty(),
        List.of());
    ClassDef klass = new ClassDef(
        "A",
        Optional.empty(),
        List.of(),
        constructor,
        List.of(method));
    Program program = new Program(List.of(klass), List.of());
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testThisUsedOutsideMethodThrows() {
    Exp badExp = new ThisExp();
    Program program = new Program(List.of(), List.of(new PrintStmt(new PrintlnExp(badExp))));
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  @Test
  public void testThisIsInitializedInMethod() {
    MethodDef method = new MethodDef(
        "foo",
        List.of(),
        "Void",
        List.of(new ExprStmt(new VarExp("this"))));
    ConstructorDef constructor = new ConstructorDef(
        List.of(),
        Optional.empty(),
        List.of());
    ClassDef klass = new ClassDef(
        "A",
        Optional.empty(),
        List.of(),
        constructor,
        List.of(method));
    Program program = new Program(List.of(klass), List.of());
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  //////////////////////////////////////
  @Test
  public void testIntMethodWithCorrectReturn() {
    MethodDef method = new MethodDef(
        "getVal",
        List.of(),
        "Int",
        List.of(new ReturnStmt(Optional.of(new IntLiteralExp(5)))));
    ConstructorDef constructor = new ConstructorDef(List.of(), Optional.empty(), List.of());
    ClassDef klass = new ClassDef("A", Optional.empty(), List.of(), constructor, List.of(method));
    Program program = new Program(List.of(klass), List.of());
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testIntMethodWithoutReturnThrows() {
    MethodDef method = new MethodDef(
        "getVal",
        List.of(),
        "Int",
        List.of(new ExprStmt(new IntLiteralExp(5))) // no return
    );
    ConstructorDef constructor = new ConstructorDef(List.of(), Optional.empty(), List.of());
    ClassDef klass = new ClassDef("A", Optional.empty(), List.of(), constructor, List.of(method));
    Program program = new Program(List.of(klass), List.of());
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  @Test
  public void testReturnWithWrongTypeThrows() {
    MethodDef method = new MethodDef(
        "getVal",
        List.of(),
        "Int",
        List.of(new ReturnStmt(Optional.of(new StringLiteralExp("wrong")))) // wrong type
    );
    ConstructorDef constructor = new ConstructorDef(List.of(), Optional.empty(), List.of());
    ClassDef klass = new ClassDef("A", Optional.empty(), List.of(), constructor, List.of(method));
    Program program = new Program(List.of(klass), List.of());
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }
  /////////////////////////////////////////////////////

  @Test
  public void testValidSuperConstructorCall() {
    ConstructorDef superCtor = new ConstructorDef(
        List.of(new VarDecStmt("Int", "x")),
        Optional.empty(),
        List.of());
    ClassDef superClass = new ClassDef("Base", Optional.empty(), List.of(), superCtor, List.of());

    ConstructorDef subCtor = new ConstructorDef(
        List.of(),
        Optional.of(List.of(new IntLiteralExp(42))), // matches super(x: Int)
        List.of());
    ClassDef subClass = new ClassDef("Sub", Optional.of("Base"), List.of(), subCtor, List.of());

    Program program = new Program(List.of(superClass, subClass), List.of());
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testSuperCallWithWrongArgCountThrows() {
    ConstructorDef superCtor = new ConstructorDef(
        List.of(new VarDecStmt("Int", "x")),
        Optional.empty(),
        List.of());
    ClassDef superClass = new ClassDef("Base", Optional.empty(), List.of(), superCtor, List.of());

    ConstructorDef subCtor = new ConstructorDef(
        List.of(),
        Optional.of(List.of()), // missing required arg
        List.of());
    ClassDef subClass = new ClassDef("Sub", Optional.of("Base"), List.of(), subCtor, List.of());

    Program program = new Program(List.of(superClass, subClass), List.of());
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  @Test
  public void testSuperCallInBaseClassThrows() {
    ConstructorDef ctor = new ConstructorDef(
        List.of(),
        Optional.of(List.of(new IntLiteralExp(1))), // invalid: no superclass
        List.of());
    ClassDef base = new ClassDef("Base", Optional.empty(), List.of(), ctor, List.of());

    Program program = new Program(List.of(base), List.of());
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }

  ////////////////////////////////////
  @Test
  public void testSubtypeAssignmentAllowed() {
    ConstructorDef superCtor = new ConstructorDef(List.of(), Optional.empty(), List.of());
    ConstructorDef subCtor = new ConstructorDef(List.of(), Optional.empty(), List.of());

    ClassDef superClass = new ClassDef("Animal", Optional.empty(), List.of(), superCtor, List.of());
    ClassDef subClass = new ClassDef("Dog", Optional.of("Animal"), List.of(), subCtor, List.of());

    VarDecStmt var = new VarDecStmt("Animal", "a");
    AssignStmt assign = new AssignStmt("a", new NewObjectExp("Dog", List.of()));
    Program program = new Program(List.of(superClass, subClass), List.of(var, assign));
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testSubtypeReturnAllowed() {
    ConstructorDef baseCtor = new ConstructorDef(List.of(), Optional.empty(), List.of());
    ConstructorDef subCtor = new ConstructorDef(List.of(), Optional.empty(), List.of());

    ClassDef base = new ClassDef("Base", Optional.empty(), List.of(), baseCtor, List.of());
    ClassDef sub = new ClassDef("Sub", Optional.of("Base"), List.of(), subCtor, List.of());

    MethodDef method = new MethodDef(
        "foo",
        List.of(),
        "Base",
        List.of(new ReturnStmt(Optional.of(new NewObjectExp("Sub", List.of())))) // Sub <: Base
    );
    ClassDef caller = new ClassDef("Caller", Optional.empty(), List.of(),
        new ConstructorDef(List.of(), Optional.empty(), List.of()), List.of(method));

    Program program = new Program(List.of(base, sub, caller), List.of());
    assertDoesNotThrow(() -> new TypeChecker().check(program));
  }

  @Test
  public void testIncompatibleAssignmentThrows() {
    ConstructorDef baseCtor = new ConstructorDef(List.of(), Optional.empty(), List.of());
    ConstructorDef subCtor = new ConstructorDef(List.of(), Optional.empty(), List.of());

    ClassDef base = new ClassDef("Base", Optional.empty(), List.of(), baseCtor, List.of());
    ClassDef sub = new ClassDef("Sub", Optional.of("Base"), List.of(), subCtor, List.of());

    VarDecStmt var = new VarDecStmt("Sub", "s");
    AssignStmt assign = new AssignStmt("s", new NewObjectExp("Base", List.of())); // Base is not a Sub
    Program program = new Program(List.of(base, sub), List.of(var, assign));
    assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
  }
//////////////////////////////////////////

@Test
public void testValidMethodOverride() {
  MethodDef superMethod = new MethodDef(
      "foo",
      List.of(new VarDecStmt("Int", "x")),
      "Int",
      List.of(new ReturnStmt(Optional.of(new VarExp("x"))))
  );
  MethodDef subMethod = new MethodDef(
      "foo",
      List.of(new VarDecStmt("Int", "x")),
      "Int",
      List.of(new ReturnStmt(Optional.of(new VarExp("x"))))
  );

  ConstructorDef ctor = new ConstructorDef(List.of(), Optional.empty(), List.of());

  ClassDef base = new ClassDef("Base", Optional.empty(), List.of(), ctor, List.of(superMethod));
  ClassDef child = new ClassDef("Child", Optional.of("Base"), List.of(), ctor, List.of(subMethod));

  Program program = new Program(List.of(base, child), List.of());
  assertDoesNotThrow(() -> new TypeChecker().check(program));
}

@Test
public void testOverrideWithWrongReturnTypeThrows() {
  MethodDef superMethod = new MethodDef(
      "foo",
      List.of(),
      "Int",
      List.of(new ReturnStmt(Optional.of(new IntLiteralExp(1))))
  );
  MethodDef subMethod = new MethodDef(
      "foo",
      List.of(),
      "String", // invalid return override
      List.of(new ReturnStmt(Optional.of(new StringLiteralExp("bad"))))
  );

  ConstructorDef ctor = new ConstructorDef(List.of(), Optional.empty(), List.of());

  ClassDef base = new ClassDef("Base", Optional.empty(), List.of(), ctor, List.of(superMethod));
  ClassDef child = new ClassDef("Child", Optional.of("Base"), List.of(), ctor, List.of(subMethod));

  Program program = new Program(List.of(base, child), List.of());
  assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
}

@Test
public void testOverrideWithMismatchedParamsThrows() {
  MethodDef superMethod = new MethodDef(
      "foo",
      List.of(new VarDecStmt("Int", "x")),
      "Void",
      List.of()
  );
  MethodDef subMethod = new MethodDef(
      "foo",
      List.of(new VarDecStmt("String", "x")), // mismatched param type
      "Void",
      List.of()
  );

  ConstructorDef ctor = new ConstructorDef(List.of(), Optional.empty(), List.of());

  ClassDef base = new ClassDef("Base", Optional.empty(), List.of(), ctor, List.of(superMethod));
  ClassDef child = new ClassDef("Child", Optional.of("Base"), List.of(), ctor, List.of(subMethod));

  Program program = new Program(List.of(base, child), List.of());
  assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
}
////////////////////////////
@Test
public void testChainedMethodCallWithSubtypes() {
  MethodDef getDog = new MethodDef(
      "getDog",
      List.of(),
      "Dog",
      List.of(new ReturnStmt(Optional.of(new NewObjectExp("Dog", List.of()))))
  );
  MethodDef speak = new MethodDef(
      "speak",
      List.of(),
      "Void",
      List.of()
  );

  ConstructorDef ctor = new ConstructorDef(List.of(), Optional.empty(), List.of());

  ClassDef animal = new ClassDef("Animal", Optional.empty(), List.of(), ctor, List.of(speak));
  ClassDef dog = new ClassDef("Dog", Optional.of("Animal"), List.of(), ctor, List.of());
  ClassDef main = new ClassDef("Main", Optional.empty(), List.of(), ctor, List.of(getDog));

  Exp chainCall = new CallMethodExp(
      new CallMethodExp(new NewObjectExp("Main", List.of()), "getDog", List.of()),
      "speak",
      List.of()
  );

  Program program = new Program(List.of(animal, dog, main), List.of(new PrintStmt(new PrintlnExp(chainCall))));
  assertDoesNotThrow(() -> new TypeChecker().check(program));
}
//////////////////////////
@Test
public void testDuplicateFieldThrows() {
  VarDecStmt field = new VarDecStmt("Int", "x");
  ClassDef klass = new ClassDef("C", Optional.empty(), List.of(field, field),
      new ConstructorDef(List.of(), Optional.empty(), List.of()), List.of());
  Program program = new Program(List.of(klass), List.of());
  assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
}

@Test
public void testDuplicateMethodThrows() {
  MethodDef method = new MethodDef("m", List.of(), "Void", List.of());
  ClassDef klass = new ClassDef("C", Optional.empty(), List.of(),
      new ConstructorDef(List.of(), Optional.empty(), List.of()), List.of(method, method));
  Program program = new Program(List.of(klass), List.of());
  assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
}

@Test
public void testDuplicateClassThrows() {
  ClassDef a1 = new ClassDef("A", Optional.empty(), List.of(),
      new ConstructorDef(List.of(), Optional.empty(), List.of()), List.of());
  ClassDef a2 = new ClassDef("A", Optional.empty(), List.of(),
      new ConstructorDef(List.of(), Optional.empty(), List.of()), List.of());
  Program program = new Program(List.of(a1, a2), List.of());
  assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
}
/////////////////////////////////////////
@Test
public void testNestedBlockScopes() {
  VarDecStmt outer = new VarDecStmt("Int", "x");
  VarDecStmt inner = new VarDecStmt("String", "x"); // shadowed

  Stmt block = new BlockStmt(List.of(
      outer,
      new BlockStmt(List.of(
          inner,
          new PrintStmt(new PrintlnExp(new VarExp("x"))) // should refer to String
      )),
      new PrintStmt(new PrintlnExp(new VarExp("x"))) // should refer to Int
  ));

  Program program = new Program(List.of(), List.of(block));
  assertDoesNotThrow(() -> new TypeChecker().check(program));
}
////////////////////////
@Test
public void testUseOfUninitializedVarThrows() {
  VarDecStmt decl = new VarDecStmt("Int", "x");
  PrintStmt use = new PrintStmt(new PrintlnExp(new VarExp("x")));
  Program program = new Program(List.of(), List.of(decl, use));
  assertThrows(RuntimeException.class, () -> new TypeChecker().check(program));
}

}
