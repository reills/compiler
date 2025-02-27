package com.classhole.compiler;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.Tokenizer;

import java.util.Optional;

public class Main {
  public static void main(String[] args) {
    String code = "class Example { Int x = 5; }";
    Tokenizer tokenizer = new Tokenizer(code);

    while (true) {
      Optional<Token> token = tokenizer.nextToken();
      if (token.isEmpty()) break;
      System.out.println(token.get());
    }
  }
}
