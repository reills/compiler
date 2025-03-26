package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.Tokenizer;
import com.classhole.compiler.parser.ast.Program;


import java.text.ParseException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ParserTest {

  private Program parse(String source) throws ParseException {
    Tokenizer tokenizer = new Tokenizer(source);
    //oops tokenizer is making arrayList.. convert it to Token[]
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
}
