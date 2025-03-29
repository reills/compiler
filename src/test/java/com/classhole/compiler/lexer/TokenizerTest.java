package com.classhole.compiler.lexer;

import com.classhole.compiler.lexer.delimiters.DotToken;
import com.classhole.compiler.lexer.keywords.*;
import com.classhole.compiler.lexer.literals.*;
import com.classhole.compiler.lexer.operators.*;
import com.classhole.compiler.lexer.primitives.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TokenizerTest {

  private void assertToken(Token token, String expectedLexeme) {
    assertNotNull(token, "Expected token but found null");
    assertEquals(expectedLexeme, token.getLexeme(), "Unexpected lexeme");
  }

  @Test
  public void testSkippingWhitespace() {
    Tokenizer tokenizer = new Tokenizer("     ");
    // No tokens should be found, and position should move after skipping whitespace
    tokenizer.skipWhitespace();
    assertEquals(5, tokenizer.getPosition());
    // Next token should be empty
    assertTrue(tokenizer.nextToken().isEmpty());
  }

  @Test
  public void testSkippingWhitespaceAndCharacter() {
    Tokenizer tokenizer = new Tokenizer("     class");
    tokenizer.skipWhitespace();
    assertEquals(5, tokenizer.getPosition());
  }

  @Test
  public void testKeywordToken() {
    Tokenizer tokenizer = new Tokenizer("class");
    assertToken(tokenizer.nextToken().orElseThrow(), "class");
  }

  @Test
  public void testIntegerLiteral() {
    Tokenizer tokenizer = new Tokenizer("123");
    assertToken(tokenizer.nextToken().orElseThrow(), "123");
  }

  @Test
  public void testBooleanLiterals() {
    Tokenizer tokenizer = new Tokenizer("true false");
    assertToken(tokenizer.nextToken().orElseThrow(), "true");
    assertToken(tokenizer.nextToken().orElseThrow(), "false");
  }

  @Test
  public void testStringLiteral() {
    Tokenizer tokenizer = new Tokenizer("\"hello\"");
    assertToken(tokenizer.nextToken().orElseThrow(), "hello");
  }

  @Test
  public void testIdentifierToken() {
    Tokenizer tokenizer = new Tokenizer("variableName");
    assertToken(tokenizer.nextToken().orElseThrow(), "variableName");
  }

  @Test
  public void testOperators() {
    Tokenizer tokenizer = new Tokenizer("+ - * / =");
    assertToken(tokenizer.nextToken().orElseThrow(), "+");
    assertToken(tokenizer.nextToken().orElseThrow(), "-");
    assertToken(tokenizer.nextToken().orElseThrow(), "*");
    assertToken(tokenizer.nextToken().orElseThrow(), "/");
    assertToken(tokenizer.nextToken().orElseThrow(), "=");
  }

  @Test
  public void testDelimiters() {
    Tokenizer tokenizer = new Tokenizer("(){};,");
    assertToken(tokenizer.nextToken().orElseThrow(), "(");
    assertToken(tokenizer.nextToken().orElseThrow(), ")");
    assertToken(tokenizer.nextToken().orElseThrow(), "{");
    assertToken(tokenizer.nextToken().orElseThrow(), "}");
    assertToken(tokenizer.nextToken().orElseThrow(), ";");
    assertToken(tokenizer.nextToken().orElseThrow(), ",");
  }

  @Test
  public void testMultipleTokens() {
    Tokenizer tokenizer = new Tokenizer("class     Example \n { Int x = 10; }");
    assertToken(tokenizer.nextToken().orElseThrow(), "class");
    assertToken(tokenizer.nextToken().orElseThrow(), "Example");
    assertToken(tokenizer.nextToken().orElseThrow(), "{");
    assertToken(tokenizer.nextToken().orElseThrow(), "Int");
    assertToken(tokenizer.nextToken().orElseThrow(), "x");
    assertToken(tokenizer.nextToken().orElseThrow(), "=");
    assertToken(tokenizer.nextToken().orElseThrow(), "10");
    assertToken(tokenizer.nextToken().orElseThrow(), ";");
    assertToken(tokenizer.nextToken().orElseThrow(), "}");
  }

  @Test
  public void testIllegalToken() {
    Tokenizer tokenizer = new Tokenizer("@");
    Exception exception = assertThrows(IllegalStateException.class, tokenizer::nextToken);
    assertEquals("Unexpected character at line 1: @", exception.getMessage());
  }

  /**
   * 1) Test all remaining keywords to cover the entire switch in createKeywordToken.
   */
  @Test
  public void testAllKeywords() {
    String code = "class extends method init return if else while break new super this println";
    Tokenizer tokenizer = new Tokenizer(code);

    // "class"
    assertInstanceOf(ClassToken.class, tokenizer.nextToken().orElseThrow());
    // "extends"
    assertInstanceOf(ExtendsToken.class, tokenizer.nextToken().orElseThrow());
    // "method"
    assertInstanceOf(MethodToken.class, tokenizer.nextToken().orElseThrow());
    // "init"
    assertInstanceOf(InitToken.class, tokenizer.nextToken().orElseThrow());
    // "return"
    assertInstanceOf(ReturnToken.class, tokenizer.nextToken().orElseThrow());
    // "if"
    assertInstanceOf(IfToken.class, tokenizer.nextToken().orElseThrow());
    // "else"
    assertInstanceOf(ElseToken.class, tokenizer.nextToken().orElseThrow());
    // "while"
    assertInstanceOf(WhileToken.class, tokenizer.nextToken().orElseThrow());
    // "break"
    assertInstanceOf(BreakToken.class, tokenizer.nextToken().orElseThrow());
    // "new"
    assertInstanceOf(NewToken.class, tokenizer.nextToken().orElseThrow());
    // "super"
    assertInstanceOf(SuperToken.class, tokenizer.nextToken().orElseThrow());
    // "this"
    assertInstanceOf(ThisToken.class, tokenizer.nextToken().orElseThrow());
    // "println"
    assertInstanceOf(PrintlnToken.class, tokenizer.nextToken().orElseThrow());
  }

  /**
   * 2) Test all primitives: Int, Boolean, Void.
   */
  @Test
  public void testPrimitiveTokens() {
    Tokenizer tokenizer = new Tokenizer("Int Boolean Void");
    // Int
    assertInstanceOf(IntTypeToken.class, tokenizer.nextToken().orElseThrow());
    // Boolean
    assertInstanceOf(BooleanTypeToken.class, tokenizer.nextToken().orElseThrow());
    // Void
    assertInstanceOf(VoidTypeToken.class, tokenizer.nextToken().orElseThrow());
  }

  /**
   * 3) Test two-character operators and leftover comparison ops: ==, !=, <=, >=, <, >.
   */
  @Test
  public void testDoubleCharOperators() {
    Tokenizer tokenizer = new Tokenizer("== != <= >= < >");
    assertInstanceOf(EqualsToken.class, tokenizer.nextToken().orElseThrow());
    assertInstanceOf(NotEqualsToken.class, tokenizer.nextToken().orElseThrow());
    assertInstanceOf(LessEqualToken.class, tokenizer.nextToken().orElseThrow());
    assertInstanceOf(GreaterEqualToken.class, tokenizer.nextToken().orElseThrow());
    assertInstanceOf(LessThanToken.class, tokenizer.nextToken().orElseThrow());
    assertInstanceOf(GreaterThanToken.class, tokenizer.nextToken().orElseThrow());
  }

  /**
   * 4) Test the dot delimiter ('.') which wasn't included in the original testDelimiters.
   */
  @Test
  public void testDotDelimiter() {
    Tokenizer tokenizer = new Tokenizer(".");
    Token token = tokenizer.nextToken().orElseThrow();
    // Ensure it's recognized as a DotToken
    assertEquals(".", token.getLexeme());
    assertInstanceOf(DotToken.class, token);
  }


  /**
   * 5) Test a multi-line string to ensure line increments inside the string literal are covered.
   */
  @Test
  public void testMultiLineStringLiteral() {
    Tokenizer tokenizer = new Tokenizer("\"Hello\nWorld\"");
    Token token = tokenizer.nextToken().orElseThrow();
    assertInstanceOf(StringLiteralToken.class, token);
    assertEquals("Hello\nWorld", token.getLexeme());
  }

  /**
   * 6) Test an unterminated string to cover the throw new IllegalStateException("Unterminated string literal").
   */
  @Test
  public void testUnterminatedString() {
    Tokenizer tokenizer = new Tokenizer("\"Hello");
    Exception exception = assertThrows(IllegalStateException.class, tokenizer::nextToken);
    assertEquals("Unterminated string literal at line 1", exception.getMessage());
  }

  @Test
  public void testTokenizerMethod() {
    // Given some sample input that should produce multiple tokens
    String input = "class Example { Int x = 10; }";
    Tokenizer tokenizer = new Tokenizer(input);
    ArrayList<Token> tokens = tokenizer.tokenize();

    String[] expectedLexemes = {
        "class",  // keyword
        "Example", // identifier
        "{",      // delimiter
        "Int",    // primitive
        "x",      // identifier
        "=",      // operator
        "10",     // integer literal
        ";",      // delimiter
        "}"       // delimiter
    };

    // check the number of tokens
    assertEquals(expectedLexemes.length, tokens.size(),
            "Mismatch in the number of tokens returned by tokenize()");

    // check each token is correctly mapped
    for (int i = 0; i < expectedLexemes.length; i++) {
      assertEquals(expectedLexemes[i], tokens.get(i).getLexeme(),
              "Unexpected lexeme at token index " + i);
    }
  }
  @Test
  public void testKeywordFollowedByIdentifier() {
    Tokenizer tokenizer = new Tokenizer("classy");
    Token token = tokenizer.nextToken().orElseThrow();
    assertInstanceOf(IdentifierToken.class, token);
    assertEquals("classy", token.getLexeme());
  }
  
  @Test
  public void testEmptyStringLiteral() {
    Tokenizer tokenizer = new Tokenizer("\"\"");
    Token token = tokenizer.nextToken().orElseThrow();
    assertEquals("", token.getLexeme());
    assertInstanceOf(StringLiteralToken.class, token);
  }

  @Test
  public void testLargeInteger() {
    assertThrows(IllegalArgumentException.class, () -> {
      Tokenizer tokenizer = new Tokenizer("12345678901234567890");
      tokenizer.nextToken().orElseThrow();
    });
    // Will throw if parseInt fails—good stress test
    // since int is too big
  }

  @Test
  public void testLineAndColumnTracking() {
    try {
      String input = Files.readString(Path.of("test_input.txt"));
      Tokenizer tokenizer = new Tokenizer(input);
      assertAll("Line and Column Tracking",
              () -> {
                assertEquals(1, tokenizer.getLine());
                assertEquals(1, tokenizer.getColumn());
                Token token1 = tokenizer.nextToken().orElseThrow();  // class
              },
              () -> {
                assertEquals(1, tokenizer.getLine());
                assertEquals(7, tokenizer.getColumn());
                Token token2 = tokenizer.nextToken().orElseThrow();  // Example
              },
              () -> {
                assertEquals(1, tokenizer.getLine());
                assertEquals(15, tokenizer.getColumn());
                Token token3 = tokenizer.nextToken().orElseThrow();  // {
              },
              () -> {
                assertEquals(2, tokenizer.getLine());
                assertEquals(3, tokenizer.getColumn());
                Token token4 = tokenizer.nextToken().orElseThrow();  // Int
              },
              () -> {
                assertEquals(2, tokenizer.getLine());
                assertEquals(7, tokenizer.getColumn());
                Token token5 = tokenizer.nextToken().orElseThrow();  // x
              },
              () -> {
                assertEquals(2, tokenizer.getLine());
                assertEquals(9, tokenizer.getColumn());
                Token token6 = tokenizer.nextToken().orElseThrow();  // =
              },
              () -> {
                assertEquals(2, tokenizer.getLine());
                assertEquals(11, tokenizer.getColumn());
                Token token7 = tokenizer.nextToken().orElseThrow();  // 10
              },
              () -> {
                assertEquals(2, tokenizer.getLine());
                assertEquals(13, tokenizer.getColumn());
                Token token8 = tokenizer.nextToken().orElseThrow();  // ;
              },
              () -> {
                assertEquals(3, tokenizer.getLine());
                assertEquals(3, tokenizer.getColumn());
                Token token9 = tokenizer.nextToken().orElseThrow();  // println
              },
              () -> {
                assertEquals(3, tokenizer.getLine());
                assertEquals(11, tokenizer.getColumn());
                Token token10 = tokenizer.nextToken().orElseThrow();  // (
              },
              () -> {
                assertEquals(3, tokenizer.getLine());
                assertEquals(12, tokenizer.getColumn());
                Token token11 = tokenizer.nextToken().orElseThrow();  // "Hello, world"
              },
              () -> {
                assertEquals(3, tokenizer.getLine());
                assertEquals(26, tokenizer.getColumn());
                Token token12 = tokenizer.nextToken().orElseThrow();  // )
              },
              () -> {
                assertEquals(3, tokenizer.getLine());
                assertEquals(27, tokenizer.getColumn());
                Token token13 = tokenizer.nextToken().orElseThrow();  // ;
              },
              () -> {
                assertEquals(4, tokenizer.getLine());
                assertEquals(1, tokenizer.getColumn());
                Token token14 = tokenizer.nextToken().orElseThrow();  // }
              }
      );
    } catch (Exception e) {
      System.out.println("io error");
    }
  }
}