package com.classhole.compiler.lexer.keywords;

import com.classhole.compiler.lexer.Token;

public record MethodToken(int line, int column) implements Token {
  @Override
  public String getLexeme() { return "method"; }
}