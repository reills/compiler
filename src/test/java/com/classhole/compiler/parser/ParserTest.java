package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.Tokenizer;
import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.Program;


import java.text.ParseException;

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
    //oops tokenizer is making arrayList.. convert it to Token[]
    Token[] tokens = tokenizer.tokenize().toArray(new Token[0]);

    Parser parser = new Parser(tokens);
    return parser.parseWholeProgram();
  }
 ////////////////////////////////////////////////////////////
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
  public void testChainedBinaryExpression() throws ParseException {
    String code = "1 + 2 * 3 - 4 / 2;";
    Program program = parse(code);
    System.out.println(program);
    assertNotNull(program);
  }
 ///////////////////////////////////////////////
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
}