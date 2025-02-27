package com.classhole.compiler.lexer.delimiters;

import com.classhole.compiler.lexer.Token;

public record EndOfFileToken(int line, int column) implements Token {
  @Override
  public String getLexeme() { return "EOF"; }
}