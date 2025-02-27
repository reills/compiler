package com.classhole.compiler.lexer.operators;

import com.classhole.compiler.lexer.Token;

public record EqualsToken(int line, int column) implements Token {
  @Override
  public String getLexeme() { return "=="; }
}
