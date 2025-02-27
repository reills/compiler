package com.classhole.compiler.lexer.primitives;

import com.classhole.compiler.lexer.Token;

public record BooleanTypeToken(int line, int column) implements Token {
  @Override
  public String getLexeme() { return "Boolean"; }
}