package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.Tokenizer;
import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.nodes.definitions.ConstructorDef;
import com.classhole.compiler.parser.ast.nodes.statements.AssignStmt;
import com.classhole.compiler.parser.ast.nodes.statements.BlockStmt;
import com.classhole.compiler.parser.ast.nodes.statements.BreakStmt;
import com.classhole.compiler.parser.ast.nodes.statements.ExprStmt;
import com.classhole.compiler.parser.ast.nodes.statements.IfStmt;
import com.classhole.compiler.parser.ast.nodes.statements.ReturnStmt;
import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;
import com.classhole.compiler.parser.ast.nodes.statements.WhileStmt;

import java.text.ParseException;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

  private void assertExpressionSource(String source, String expected) throws ParseException {
    Tokenizer tokenizer = new Tokenizer(source);
    Token[] tokens = tokenizer.tokenize().toArray(new Token[0]);
    Parser parser = new Parser(tokens);
    ParseResult<Exp> parsed = ExpressionParser.exp(parser, 0);

    Exp expr = parsed.result();
    assertNotNull(expr, "Expected expression but got null");
    assertEquals(expected, expr.toString(), "Parsed expression does not match expected structure");
  }

  private Program parse(String source) throws ParseException {
    Tokenizer tokenizer = new Tokenizer(source);
    // oops tokenizer is making arrayList.. convert it to Token[]
    Token[] tokens = tokenizer.tokenize().toArray(new Token[0]);

    Parser parser = new Parser(tokens);
    return parser.parseWholeProgram();
  }

  @Test
  public void testSimpleClass() throws ParseException {
    String code = """
          class Animal {
            Int age;
            init() {}
            method speak() Void {
              return println(0);
            }
          }

          Animal a;
          a = new Animal();
          a.speak();
        """;

    Program program = parse(code);
    assertNotNull(program, "Program should not be null");
    assertFalse(program.classes().isEmpty(), "Should contain at least one class");
    assertFalse(program.entryPoint().isEmpty(), "Should contain entry point statements");

  }

  @Test
  public void testInheritance() throws ParseException {
    String code = """
          class Animal {
            init() {}
            method speak() Void { return println(0); }
          }

          class Cat extends Animal {
            init() { super(); }
            method speak() Void { return println(1); }
          }

          Animal cat;
          cat = new Cat();
          cat.speak();
        """;

    Program program = parse(code);
    assertEquals(2, program.classes().size());
    assertTrue(program.entryPoint().size() >= 3);
  }

  @Test
  public void testEmptyConstructorWithFields() throws ParseException {
    String code = """
          class Dog {
            Int barkCount;
            init() {}
            method bark() Void { return println(2); }
          }

          Dog d;
          d = new Dog();
          d.bark();
        """;

    Program program = parse(code);
    assertEquals(1, program.classes().size());
  }

  @Test
  public void testMissingSemicolonFails() {
    String bad = """
          class A {
            init() {}
          }

          Int x
          x = 1;
        """;

    assertThrows(ParseException.class, () -> parse(bad));
  }

  @Test
  public void testBlockStatement() throws ParseException {
    String code = """
          {
            return;
          }
        """;
    Program program = parse(code); // Dummy class needed
    assertNotNull(program);
  }

  @Test
  public void testWhileLoop() throws ParseException {
    String code = """
          {
            while (true) {
              return;
            }
          }
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testIfStatement() throws ParseException {
    String code = """
          {
            if (true) {
              return;
            }
          }
        """;
    Program program = parse("class A { init() {} } " + code);
    assertNotNull(program);
  }

  @Test
  public void testIfElseStatement() throws ParseException {
    String code = """
          {
            if (false) {
              return;
            } else {
              return;
            }
          }
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testBreakStatement() throws ParseException {
    String code = """
          {
            while (true) {
              break;
            }
          }
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testBinaryPlusExpression() throws ParseException {
    String code = "5 + 3;";
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testBinaryMultExpression() throws ParseException {
    String code = "5 * 3;";
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testInvalidMethodCall_MissingRightParen() {
    String code = "badCall.method(;";
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testStringLiteralExpression() throws ParseException {
    String code = """
            println("Hello, world!");
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testEmptyConstructorArguments() throws ParseException {
    String code = """
          class Test {
            init() {}
          }

          Test t;
          t = new Test();
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testParenthesizedExpression() throws ParseException {
    String code = "(5 + 3) * 2;";
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testMultipleCommaExpression() throws ParseException {
    String code = """
              class Animal {
          Int age;
          init() {}
          method speak(Int a, Int b) Void {
            return println(0);
          }
        }

        Animal a;
        a = new Animal(1, 2);
        a.speak(3, 4);
              """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testConstructorWithSuperCall() throws ParseException {
    String code = """
          class Foo extends Bar {
            int x;
            init(int y) {
              super(1, 2);
              return;
            }
          }
          Foo f;
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testClassDef_NonIdentifierClassName() {
    String code = "class 123 { init() {} } println(\"foo\");";
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testClassDef_NonIdentifierSuperClassName() {
    String code = "class MyClass extends 456 { init() {} } MyClass myClass;";
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testClassDef_FieldDeclarations() throws ParseException {
    String code = """
        class TestFields {
          int field1;
          boolean field2;
          void field3;
          CustomType field4;
          init() {}
        }
        TestFields t;
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testClassDef_InvalidFieldDeclaration() {
    String code = "class TestClass { int 123; init() {} } TestClass t;";
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testClassDef_MissingInit() {
    String code = "class TestClass { notInit() {} } TestClass t;";
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testMethodDef_NonIdentifierMethodName() {
    String code = """
        class TestClass {
          init() {}
          method 123() void {}
        }
        TestClass t;
        """;
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testMethodDef_ReturnTypes() throws ParseException {
    String code = """
        class TestClass {
          init() {}
          method method1() int { return 1; }
          method method2() boolean { return true; }
          method method3() void {}
          method method4() CustomType { return new CustomType(); }
        }
        TestClass t;
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testMethodDef_InvalidReturnType() {
    String code = """
        class TestClass {
          init() {}
          method badMethod() 123 {}
        }
        TestClass t;
        """;
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testClassDef_WithSuperConstructorCall() throws ParseException {
    String code = """
        class Child extends Parent {
          init() {
            super(1, true, "test");
          }
        }
        Child c;
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testClassDef_WithFieldsAndMethods() throws ParseException {
    // Comprehensive test covering multiple aspects of class parsing
    String code = """
        class CompleteClass extends BaseClass {
          int field1;
          boolean field2;
          CustomType field3;

          init(int param1, boolean param2) {
            super(param1, param2);
            field1 = param1;
            field2 = param2;
          }

          method calculate(int a, int b) int {
            return a + b;
          }

          method doSomething() void {
            println("Doing something");
          }
        }
        CompleteClass c;
        """;
    Program program = parse(code);
    assertNotNull(program);
  }

  @Test
  public void testChainedBinaryExpression() throws ParseException {
    String code = "1 + 2 * 3 - 4 / 2;";
    Program program = parse(code);
    //System.out.println(program);
    assertNotNull(program);
  }

  @Test
  public void testSimpleBinaryExpressionParsing() throws ParseException {
    assertExpressionSource("1 + 2;",
        "BinaryExp[left=IntLiteralExp[value=1], operator=+, right=IntLiteralExp[value=2]]");
  }

  @Test
  public void testOperatorPrecedenceParsing() throws ParseException {
    assertExpressionSource("1 + 2 * 3;",
        "BinaryExp[left=IntLiteralExp[value=1], operator=+, right=BinaryExp[left=IntLiteralExp[value=2], operator=*, right=IntLiteralExp[value=3]]]");
  }

  @Test
  public void testParenthesizedExpressionParsing() throws ParseException {
    assertExpressionSource("(1 + 2) * 3;",
        "BinaryExp[left=ParenExp[expression=BinaryExp[left=IntLiteralExp[value=1], operator=+, right=IntLiteralExp[value=2]]], operator=*, right=IntLiteralExp[value=3]]");
  }

  @Test
  public void testStringLiteralParsing() throws ParseException {
    assertExpressionSource("\"hello\";",
        "StringLiteralExp[value=hello]");
  }

  @Test
  public void testVariableExpressionParsing() throws ParseException {
    assertExpressionSource("x;",
        "VarExp[name=x]");
  }

  @Test
  public void testMethodCallParsing() throws ParseException {
    assertExpressionSource("obj.add(1, 2);",
        "CallMethodExp[target=VarExp[name=obj], methodName=add, args=[IntLiteralExp[value=1], IntLiteralExp[value=2]]]");
  }

  @Test
  public void testComplexExpression() throws ParseException {
    assertExpressionSource(
        "x + 3 * (2 - 1);",
        "BinaryExp[" +
            "left=VarExp[name=x], " +
            "operator=+, " +
            "right=BinaryExp[" +
            "left=IntLiteralExp[value=3], " +
            "operator=*, " +
            "right=ParenExp[expression=BinaryExp[" +
            "left=IntLiteralExp[value=2], " +
            "operator=-, " +
            "right=IntLiteralExp[value=1]" +
            "]]" +
            "]" +
            "]");
  }

  @Test
  public void testChainedMethodCallsParsing() throws ParseException {
    assertExpressionSource(
        "obj.foo().bar();",
        "CallMethodExp[" +
            "target=CallMethodExp[" +
            "target=VarExp[name=obj], " +
            "methodName=foo, " +
            "args=[]" +
            "], " +
            "methodName=bar, " +
            "args=[]" +
            "]");
  }

  @Test
  public void testNestedParenthesesParsing() throws ParseException {
    assertExpressionSource(
        "(((1 + 2))) * 4;",
        "BinaryExp[" +
            "left=ParenExp[expression=ParenExp[expression=ParenExp[expression=BinaryExp[" +
            "left=IntLiteralExp[value=1], " +
            "operator=+, " +
            "right=IntLiteralExp[value=2]" +
            "]]]], " +
            "operator=*, " +
            "right=IntLiteralExp[value=4]" +
            "]");
  }

  @Test
  public void testSimpleLogicalOperatorParsing() throws ParseException {
    assertExpressionSource(
        "x != 0;",
        "BinaryExp[" +
            "left=VarExp[name=x], " +
            "operator=!=, " +
            "right=IntLiteralExp[value=0]" +
            "]");
  }

  @Test
  public void testChainedLogicalOperatorsParsing() throws ParseException {
    assertExpressionSource(
        "(x + 1) < y == z >= 10;",
        "BinaryExp[" +
            "left=BinaryExp[" +
            "left=BinaryExp[" +
            "left=ParenExp[expression=BinaryExp[" +
            "left=VarExp[name=x], " +
            "operator=+, " +
            "right=IntLiteralExp[value=1]" +
            "]], " +
            "operator=<, " +
            "right=VarExp[name=y]" +
            "], " +
            "operator===, " +
            "right=VarExp[name=z]" +
            "], " +
            "operator=>=, " +
            "right=IntLiteralExp[value=10]" +
            "]");
  }

  @Test
  public void testLogicalOperatorsWithParenthesesParsing() throws ParseException {
    assertExpressionSource(
        "((x + 1) < y) == (z >= 10);",
        "BinaryExp[" +
            "left=ParenExp[expression=BinaryExp[" +
            "left=ParenExp[expression=BinaryExp[" +
            "left=VarExp[name=x], " +
            "operator=+, " +
            "right=IntLiteralExp[value=1]" +
            "]], " +
            "operator=<, " +
            "right=VarExp[name=y]" +
            "]], " +
            "operator===, " +
            "right=ParenExp[expression=BinaryExp[" +
            "left=VarExp[name=z], " +
            "operator=>=, " +
            "right=IntLiteralExp[value=10]" +
            "]]" +
            "]");
  }

  @Test
  public void testFullMixedExpressionParsing() throws ParseException {
    assertExpressionSource(
        "((1 + 2) * obj.calc(3).getValue()) == 9;",
        "BinaryExp[" +
            "left=ParenExp[expression=BinaryExp[" +
            "left=ParenExp[expression=BinaryExp[" +
            "left=IntLiteralExp[value=1], " +
            "operator=+, " +
            "right=IntLiteralExp[value=2]" +
            "]], " +
            "operator=*, " +
            "right=CallMethodExp[" +
            "target=CallMethodExp[" +
            "target=VarExp[name=obj], " +
            "methodName=calc, " +
            "args=[IntLiteralExp[value=3]]" +
            "], " +
            "methodName=getValue, " +
            "args=[]" +
            "]" +
            "]], " +
            "operator===, " +
            "right=IntLiteralExp[value=9]" +
            "]");
  }

  @Test
  public void testVarDeclarationStmt() throws ParseException {
    String code = "Int x;";
    Program program = parse(code);
    assertEquals("Int", ((VarDecStmt) program.entryPoint().get(0)).type());
    assertEquals("x", ((VarDecStmt) program.entryPoint().get(0)).name());
  }

  @Test
  public void testExprStmt() throws ParseException {
    String code = "obj.call();";
    Program program = parse(code);
    assertTrue(program.entryPoint().get(0) instanceof ExprStmt);
  }

  @Test
  public void testIfElseStmt() throws ParseException {
    String code = """
          if (true) {
            return;
          } else {
            break;
          }
        """;
    Program program = parse("class A { init() {} } " + code);
    assertNotNull(program.entryPoint().get(0));
    assertTrue(program.entryPoint().get(0) instanceof IfStmt);
  }

  @Test
  public void testWhileStmt() throws ParseException {
    String code = """
          while (x < 5) {
            x = x + 1;
          }
        """;
    Program program = parse("class A { init() {} } " + code);
    assertTrue(program.entryPoint().get(0) instanceof WhileStmt);
  }

  @Test
  public void testBlockStmt() throws ParseException {
    String code = """
        {
          Int y;
          y = 2;
        }
        """;
    Program program = parse("class A { init() {} } " + code);
    assertTrue(program.entryPoint().get(0) instanceof BlockStmt);
  }

  @Test
  public void testProgramWithOnlyStatements() throws ParseException {
    String code = """
          Int x;
          x = 10;
          println(x);
        """;
    Program program = parse(code);
    assertTrue(program.classes().isEmpty());
    assertEquals(3, program.entryPoint().size());
  }

  @Test
  public void testProgramWithClassAndStatements() throws ParseException {
    String code = """
          class Dog {
            Int age;
            init() {}
            method bark() Void { return println(1); }
          }
          Dog d;
          d = new Dog();
          d.bark();
        """;
    Program program = parse(code);
    assertEquals(1, program.classes().size());
    assertEquals(3, program.entryPoint().size());
  }

  @Test
  public void testProgramWithMultipleClasses() throws ParseException {
    String code = """
          class A {
            init() {}
          }
          class B {
            init() {}
          }
          Int x;
          x = 5;
        """;
    Program program = parse(code);
    assertEquals(2, program.classes().size());
    assertEquals(2, program.entryPoint().size());
  }

  @Test
  public void testProgramWithOnlyClassesFails() {
    String code = """
          class A {
            init() {}
          }
        """;
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testStatementsBeforeClassDefinitionFails() {
    String code = """
          Int x;
          class A {
            init() {}
          }
        """;
    assertThrows(ParseException.class, () -> parse(code));
  }

  @Test
  public void testExpectSuccess() throws ParseException {
    Token[] tokens = new Tokenizer("(").tokenize().toArray(new Token[0]);
    Parser parser = new Parser(tokens);

    Token token = ParseUtility.expect(parser, com.classhole.compiler.lexer.delimiters.LeftParenToken.class,
        "Expected '('");

    assertTrue(token instanceof com.classhole.compiler.lexer.delimiters.LeftParenToken);
    assertEquals(1, parser.getPos());
  }

  @Test
  public void testExpectFailsOnWrongTokenType() {
    Token[] tokens = new Tokenizer("42").tokenize().toArray(new Token[0]);
    Parser parser = new Parser(tokens);

    ParseException ex = assertThrows(ParseException.class,
        () -> ParseUtility.expect(parser, com.classhole.compiler.lexer.keywords.ReturnToken.class, "Expected return"));

    assertTrue(ex.getMessage().contains("Expected return"));
  }

  @Test
  public void testExpectFailsOnEmptyInput() {
    Token[] tokens = new Token[] {}; // no tokens
    Parser parser = new Parser(tokens);

    ParseException ex = assertThrows(ParseException.class, () -> ParseUtility.expect(parser,
        com.classhole.compiler.lexer.delimiters.LeftBraceToken.class, "Expected '{'"));

    assertTrue(ex.getMessage().contains("Ran out of tokens"));
  }

  @Test
  public void testVoidVarDeclaration() throws ParseException {
    String code = "Void flag;";
    Program program = parse(code);
    VarDecStmt stmt = (VarDecStmt) program.entryPoint().get(0);
    assertEquals("Void", stmt.type());
  }

  @Test
  public void testCustomTypeVarDeclaration() throws ParseException {
    String code = "CustomType x;";
    Program program = parse(code);
    VarDecStmt stmt = (VarDecStmt) program.entryPoint().get(0);
    assertEquals("CustomType", stmt.type());
  }

  @Test
  public void testAssignStatement() throws ParseException {
    String code = """
          {
            x = 5;
          }
        """;
    Program program = parse("class A { init() {} } " + code);
    AssignStmt stmt = (AssignStmt) ((BlockStmt) program.entryPoint().get(0)).statements().get(0);
    assertEquals("x", stmt.variableName());
    assertEquals("IntLiteralExp[value=5]", stmt.expression().toString());
  }

  @Test
  public void testExpressionStatement() throws ParseException {
    String code = "x + 1;";
    Program program = parse(code);
    ExprStmt stmt = (ExprStmt) program.entryPoint().get(0);
    assertEquals("BinaryExp[left=VarExp[name=x], operator=+, right=IntLiteralExp[value=1]]", stmt.exp().toString());
  }

  @Test
  public void testVarDecCustomType() throws ParseException {
    String code = "MyType val;";
    Program program = parse(code);
    VarDecStmt stmt = (VarDecStmt) program.entryPoint().get(0);
    assertEquals("MyType", stmt.type());
    assertEquals("val", stmt.name());
  }

  @Test
  public void testBreakInsideWhile() throws ParseException {
    String code = """
          while (true) {
            break;
          }
        """;
    Program program = parse("class A { init() {} } " + code);
    WhileStmt whileStmt = (WhileStmt) program.entryPoint().get(0);
    BlockStmt body = (BlockStmt) whileStmt.body();
    assertTrue(body.statements().get(0) instanceof BreakStmt);
  }

  @Test
  public void testNestedBlockStatement() throws ParseException {
    String code = """
          {
            Int x;
            x = 10;
          }
        """;
    Program program = parse("class A { init() {} } " + code);
    BlockStmt stmt = (BlockStmt) program.entryPoint().get(0);
    assertEquals(2, stmt.statements().size());
  }

  @Test
  public void testIfWithEqualityLogic() throws ParseException {
    String code = """
          if (x == 1) {
            return;
          }
        """;
    Program program = parse("class A { init() {} } " + code);

    assertTrue(program.entryPoint().get(0) instanceof IfStmt);
    IfStmt stmt = (IfStmt) program.entryPoint().get(0);

    assertEquals("BinaryExp[left=VarExp[name=x], operator===, right=IntLiteralExp[value=1]]",
        stmt.condition().toString());
  }

  @Test
  public void testWhileWithLessThanLogic() throws ParseException {
    String code = """
          while (a < b) {
            break;
          }
        """;
    Program program = parse("class A { init() {} } " + code);

    assertTrue(program.entryPoint().get(0) instanceof WhileStmt);
    WhileStmt stmt = (WhileStmt) program.entryPoint().get(0);

    assertEquals("BinaryExp[left=VarExp[name=a], operator=<, right=VarExp[name=b]]",
        stmt.condition().toString());
  }

  @Test
  public void testReturnWithGreaterEqualLogic() throws ParseException {
    String code = """
          class A {
            init() {
              return x >= 10;
            }
          }
          A a;
        """;
    Program program = parse(code);

    ReturnStmt stmt = (ReturnStmt) program.classes().get(0).constructor().body().get(0);

    assertTrue(stmt.expression().isPresent());
    assertEquals("BinaryExp[left=VarExp[name=x], operator=>=, right=IntLiteralExp[value=10]]",
        stmt.expression().get().toString());
  }

  @Test
  public void testReturnWithNotEqualsLogic() throws ParseException {
    String code = """
          class A {
            init() {
              return x != y;
            }
          }
          A a;
        """;
    Program program = parse(code);

    ReturnStmt stmt = (ReturnStmt) program.classes().get(0).constructor().body().get(0);
    assertTrue(stmt.expression().isPresent());
    assertEquals("BinaryExp[left=VarExp[name=x], operator=!=, right=VarExp[name=y]]",
        stmt.expression().get().toString());
  }

  @Test
  public void testIfWithLessThanLogic() throws ParseException {
    String code = """
          class A {
            init() {
              if (a < b) {
                return;
              }
            }
          }
          A a;
        """;
    Program program = parse(code);

    IfStmt stmt = (IfStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals("BinaryExp[left=VarExp[name=a], operator=<, right=VarExp[name=b]]", stmt.condition().toString());
  }

  @Test
  public void testWhileWithLessEqualLogic() throws ParseException {
    String code = """
          class A {
            init() {
              while (x <= 10) {
                break;
              }
            }
          }
          A a;
        """;
    Program program = parse(code);

    WhileStmt stmt = (WhileStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals("BinaryExp[left=VarExp[name=x], operator=<=, right=IntLiteralExp[value=10]]",
        stmt.condition().toString());
  }

  @Test
  public void testAssignWithLogicExpression() throws ParseException {
    String code = """
          class A {
            init() {
              result = a > b;
            }
          }
          A a;
        """;
    Program program = parse(code);

    AssignStmt stmt = (AssignStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals("result", stmt.variableName());
    assertEquals("BinaryExp[left=VarExp[name=a], operator=>, right=VarExp[name=b]]", stmt.expression().toString());
  }

  @Test
  public void testPrintlnWithLogicExpression() throws ParseException {
    String code = """
          class A {
            init() {
              println(x == 1);
            }
          }
          A a;
        """;
    Program program = parse(code);

    ExprStmt stmt = (ExprStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals("PrintlnExp[exp=BinaryExp[left=VarExp[name=x], operator===, right=IntLiteralExp[value=1]]]",
        stmt.exp().toString());
  }

  @Test
  public void testBreakStatementAgain() throws ParseException {
    String code = """
          class A {
            init() {
              break;
            }
          }
          A a;
        """;
    Program program = parse(code);

    BreakStmt stmt = (BreakStmt) program.classes().get(0).constructor().body().get(0);
    assertNotNull(stmt);
  }

  @Test
  public void testVarDeclarationCustomType() throws ParseException {
    String code = """
          class A {
            init() {
              CustomType val;
            }
          }
          A a;
        """;
    Program program = parse(code);

    VarDecStmt stmt = (VarDecStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals("CustomType", stmt.type());
    assertEquals("val", stmt.name());
  }

  @Test
  public void testSuperCallWithArgs() throws ParseException {
    String code = """
          class A extends B {
            init() {
              super(1, true, "ok");
            }
          }
          A a;
        """;
    Program program = parse(code);

    ConstructorDef constructor = program.classes().get(0).constructor();
    assertTrue(constructor.superArgs().isPresent());

    List<Exp> args = constructor.superArgs().get();
    assertEquals(3, args.size());
  }

  @Test
  public void testAssignmentStatement() throws ParseException {
    String code = """
          class A {
            init() {
              x = 42;
            }
          }
          A a;
        """;
    Program program = parse(code);

    AssignStmt stmt = (AssignStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals("x", stmt.variableName());
  }

  @Test
  public void testBlockWithMultipleStatements() throws ParseException {
    String code = """
          class A {
            init() {
              {
                Int a;
                a = 5;
              }
            }
          }
          A a;
        """;
    Program program = parse(code);

    BlockStmt block = (BlockStmt) program.classes().get(0).constructor().body().get(0);
    assertEquals(2, block.statements().size());
  }

  @Test
  public void testSuperCallNoArguments() throws ParseException {
    String code = """
          class A extends B {
            init() {
              super();
            }
          }
          A a;
        """;
    Program program = parse(code);

    assertTrue(program.classes().get(0).constructor().superArgs().isPresent());
    assertTrue(program.classes().get(0).constructor().superArgs().get().isEmpty());
  }

  @Test
  public void testInvalidStatementFallback() {
    String code = """
          class A {
            init() {
              @;
            }
          }
          A a;
        """;
    assertThrows(IllegalStateException.class, () -> parse(code));
  }

  @Test
  public void testInvalidVarDeclarationFallback() {
    String code = """
          class A {
            init() {
              Int 123;
            }
          }
          A a;
        """;
    assertThrows(ParseException.class, () -> parse(code));
  }

}