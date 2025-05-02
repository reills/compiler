package com.classhole.compiler;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.Tokenizer;
import com.classhole.compiler.parser.Parser;
import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.codegenerator.CodeGenerator;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    String code = """
      class Greeter {
        init() { }
        method greet() Void {
          return println(42);
        }
      }

      Greeter g;
      g = new Greeter();
      g.greet();
    """;

    try {
      // Step 1: Tokenize the input into a list
      Tokenizer tokenizer = new Tokenizer(code);
      List<Token> tokenList = new ArrayList<>();
      while (true) {
        var tok = tokenizer.nextToken();
        if (tok.isEmpty()) break;
        tokenList.add(tok.get());
      }

      // Step 2: Parse it
      Token[] tokenArray = tokenList.toArray(new Token[0]);
      Parser parser = new Parser(tokenArray);
      Program program = parser.parseWholeProgram();

      // Step 3: Generate JS code
      CodeGenerator codeGenerator = new CodeGenerator();
      String js = codeGenerator.generate(program);

      // Step 4: Output it
      System.out.println(js);

    } catch (ParseException e) {
      System.err.println("Parse error: " + e.getMessage());
    }
  }
}
