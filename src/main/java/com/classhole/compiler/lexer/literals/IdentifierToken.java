package com.classhole.compiler.lexer.literals;

import com.classhole.compiler.lexer.Token;

public record IdentifierToken(String name, int line, int column) implements Token {
  @Override
  public String getLexeme() { return name; }
}