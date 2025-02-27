package com.classhole.compiler.lexer;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class TokenizerTest {

  //helper
  private void assertToken(Optional<Token> token, String expectedLexeme) {
    assertTrue(token.isPresent(), "Expected token but found empty Optional");
    assertEquals(expectedLexeme, token.get().getLexeme(), "Unexpected lexeme");
  }


  @Test
  public void testKeywordToken() {
    Tokenizer tokenizer = new Tokenizer("class");
    assertToken( tokenizer.nextToken(), "class" );
  }

  @Test
  public void testIntegerLiteral() {
    Tokenizer tokenizer = new Tokenizer("123");
    assertToken( tokenizer.nextToken(), "123" );
  }

  @Test
  public void testIdentifierToken() {
    Tokenizer tokenizer = new Tokenizer("variableName");
    assertToken( tokenizer.nextToken(), "variableName" );
  }

  @Test
  public void testOperators() {
    Tokenizer tokenizer = new Tokenizer("+ - * / =");

    assertToken(tokenizer.nextToken(), "+");
    assertToken(tokenizer.nextToken(), "-");
    assertToken(tokenizer.nextToken(), "*");
    assertToken(tokenizer.nextToken(), "/");
    assertToken(tokenizer.nextToken(), "=");
  }

  @Test
  public void testDelimiters() {
    Tokenizer tokenizer = new Tokenizer("(){};,");

    assertToken(tokenizer.nextToken(), "(");
    assertToken(tokenizer.nextToken(), ")");
    assertToken(tokenizer.nextToken(), "{");
    assertToken(tokenizer.nextToken(), "}");
    assertToken(tokenizer.nextToken(), ";");
    assertToken(tokenizer.nextToken(), ",");
  }

  @Test
  public void testMultipleTokens() {
    Tokenizer tokenizer = new Tokenizer("class Example { Int x = 10; }");

    assertToken(tokenizer.nextToken(), "class");
    assertToken(tokenizer.nextToken(), "Example");
    assertToken(tokenizer.nextToken(), "{");
    assertToken(tokenizer.nextToken(), "Int");
    assertToken(tokenizer.nextToken(), "x");
    assertToken(tokenizer.nextToken(), "=");
    assertToken(tokenizer.nextToken(), "10");
    assertToken(tokenizer.nextToken(), ";");
    assertToken(tokenizer.nextToken(), "}");
  }

  @Test
  public void testIllegalToken() {
    Tokenizer tokenizer = new Tokenizer("@");

    //Optional<Token> test = tokenizer.nextToken();
    //if(test.isPresent()) {
    //  Token token = test.get();
    //  token.getLexeme();
    //}

    Exception exception = assertThrows(IllegalStateException.class, tokenizer::nextToken);
    assertEquals("Unexpected character at line 1: @", exception.getMessage());
  }

}

