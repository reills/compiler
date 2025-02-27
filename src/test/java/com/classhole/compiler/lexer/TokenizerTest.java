package com.classhole.compiler.lexer;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class TokenizerTest {

  private void assertToken(Token token, String expectedLexeme) {
    assertNotNull(token, "Expected token but found null");
    assertEquals(expectedLexeme, token.getLexeme(), "Unexpected lexeme");
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
    Tokenizer tokenizer = new Tokenizer("class Example { Int x = 10; }");
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
}
