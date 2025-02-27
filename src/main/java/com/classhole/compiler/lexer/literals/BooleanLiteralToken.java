package com.classhole.compiler.lexer.literals;


import com.classhole.compiler.lexer.Token;

public record BooleanLiteralToken(boolean value, int line, int column) implements Token {
  @Override
  public String getLexeme() { return String.valueOf(value); }
}
