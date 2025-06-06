package com.classhole.compiler.lexer.delimiters;


import com.classhole.compiler.lexer.Token;

public record RightParenToken(int line, int column) implements Token {
  @Override
  public String getLexeme() { return ")"; }
}