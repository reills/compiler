package com.classhole.compiler.lexer;

public interface Token {
  String getLexeme(); // raw string from the input "d"
  int line();      // line number for errors
  int column(); // column for errors

}