package com.classhole.compiler.lexer.primitives;

import com.classhole.compiler.lexer.Token;

public record VoidTypeToken(int line, int column) implements Token {
  @Override
  public String getLexeme() { return "Void"; }
}
