package com.classhole.compiler.lexer;

import com.classhole.compiler.lexer.keywords.*;
import com.classhole.compiler.lexer.literals.*;
import com.classhole.compiler.lexer.operators.*;
import com.classhole.compiler.lexer.primitives.*;
import com.classhole.compiler.lexer.delimiters.*;

import java.util.Optional;

public class Tokenizer {
  private final String input;
  private int position;
  private int line;
  private int column;

  public Tokenizer(String input) {
    // Append EOF marker for convenience
    this.input = input + " EOF";
    this.position = 0;
    this.line = 1;
    this.column = 1;
  }


  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public char currentChar() {
    return input.charAt(position);
  }

  private void advance() {
    position++;
    column++;
  }

  public void skipWhitespace() {
    while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
      if (input.charAt(position) == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
      position++;
    }
  }

  public Optional<Token> nextToken() {
    skipWhitespace();

    if (position >= input.length()) {
      return Optional.empty();
    }

    char current = input.charAt(position);

    // Try to match in a particular order
    Optional<Token> token = tryMatchKeyword()
        .or(this::tryMatchPrimitive)
        .or(this::tryMatchLiteral)
        .or(this::tryMatchIdentifier)
        .or(this::tryMatchOperator)
        .or(this::tryMatchDelimiter);

    if (token.isPresent()) {
      return token;
    }

    // If we get here, we found an unexpected character
    throw new IllegalStateException("Unexpected character at line " + line + ": " + current);
  }

  // --------------------------------------------------------------------
  //  1) KEYWORDS
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchKeyword() {
    String keyword = matchKeyword();
    if (keyword != null) {
      int tokenLine = line;
      int tokenColumn = column;
      // consume the keyword
      position += keyword.length();
      column += keyword.length();
      return Optional.of(createKeywordToken(keyword, tokenLine, tokenColumn));
    }
    return Optional.empty();
  }

  private String matchKeyword() {
    String[] keywords = {
        "class", "extends", "method", "init", "return", "if", "else",
        "while", "break", "new", "super", "this", "println"
    };
    for (String kw : keywords) {
      if (matchesExact(kw)) {
        return kw;
      }
    }
    return null;
  }

  private Token createKeywordToken(String lexeme, int line, int column) {
    return switch (lexeme) {
      case "class"   -> new ClassToken(line, column);
      case "extends" -> new ExtendsToken(line, column);
      case "method"  -> new MethodToken(line, column);
      case "init"    -> new InitToken(line, column);
      case "return"  -> new ReturnToken(line, column);
      case "if"      -> new IfToken(line, column);
      case "else"    -> new ElseToken(line, column);
      case "while"   -> new WhileToken(line, column);
      case "break"   -> new BreakToken(line, column);
      case "new"     -> new NewToken(line, column);
      case "super"   -> new SuperToken(line, column);
      case "this"    -> new ThisToken(line, column);
      case "println" -> new PrintlnToken(line, column);
      default        -> throw new IllegalArgumentException("Unknown keyword: " + lexeme);
    };
  }

  // --------------------------------------------------------------------
  //  2) PRIMITIVES
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchPrimitive() {
    String[] primitives = {"Int", "Boolean", "Void"};
    for (String prim : primitives) {
      if (matchesExact(prim)) {
        int tokenLine = line;
        int tokenColumn = column;
        position += prim.length();
        column += prim.length();
        return Optional.of(createPrimitiveToken(prim, tokenLine, tokenColumn));
      }
    }
    return Optional.empty();
  }

  private Token createPrimitiveToken(String lexeme, int line, int column) {
    return switch (lexeme) {
      case "Int"     -> new IntTypeToken(line, column);
      case "Boolean" -> new BooleanTypeToken(line, column);
      case "Void"    -> new VoidTypeToken(line, column);
      default        -> throw new IllegalArgumentException("Unknown primitive type: " + lexeme);
    };
  }

  // --------------------------------------------------------------------
  //  3) LITERALS (e.g. integers)
  // --------------------------------------------------------------------

  // --------------------------------------------------------------------
  //  3) LITERALS (e.g. integers, booleans, strings)
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchLiteral() {
    // 3.1 Integers
    if (Character.isDigit(currentChar())) {
      int tokenLine = line;
      int tokenColumn = column;
      int start = position;
      while (position < input.length() && Character.isDigit(input.charAt(position))) {
        advance();
      }
      int lexeme = Integer.parseInt(input.substring(start, position));
      return Optional.of(new IntegerLiteralToken(lexeme, tokenLine, tokenColumn));
    }

    // 3.2 Booleans
    if (matchesExact("true")) {
      int tokenLine = line;
      int tokenColumn = column;
      position += 4; // length of "true"
      column += 4;
      return Optional.of(new BooleanLiteralToken(true, tokenLine, tokenColumn));
    }
    if (matchesExact("false")) {
      int tokenLine = line;
      int tokenColumn = column;
      position += 5; // length of "false"
      column += 5;
      return Optional.of(new BooleanLiteralToken(false, tokenLine, tokenColumn));
    }

    // 3.3 Strings
    if (currentChar() == '\"') {
      int tokenLine = line;
      int tokenColumn = column;
      advance(); // skip opening quote
      int start = position;

      // Gather characters until the next quote or EOF
      while (position < input.length() && input.charAt(position) != '\"') {
        // ignore new lines for multi-line strings, handle line changes here
        if (input.charAt(position) == '\n') {
          line++;
          column = 1;
        } else {
          column++;
        }
        position++;
      }

      if (position >= input.length()) {
        // Reached EOF without closing quote
        throw new IllegalStateException("Unterminated string literal at line " + tokenLine);
      }
      // Extract the string between the quotes
      String value = input.substring(start, position);

      // Skip the closing quote
      advance();
      column++;

      return Optional.of(new StringLiteralToken(value, tokenLine, tokenColumn));
    }

    return Optional.empty();
  }

  // --------------------------------------------------------------------
  //  4) IDENTIFIERS
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchIdentifier() {
    if (Character.isLetter(currentChar())) {
      int tokenLine = line;
      int tokenColumn = column;
      int start = position;
      while (position < input.length() && Character.isLetterOrDigit(input.charAt(position))) {
        advance();
      }
      String lexeme = input.substring(start, position);
      // Again, (line, column, lexeme) for dynamic tokens
      return Optional.of(new IdentifierToken(lexeme, tokenLine, tokenColumn));
    }
    return Optional.empty();
  }

  // --------------------------------------------------------------------
  //  5) OPERATORS
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchOperator() {
    // Check for two-char operators first
    if (matchesTwoCharOperator()) {
      int tokenLine = line;
      int tokenColumn = column;
      String lexeme = input.substring(position, position + 2);
      position += 2;
      column += 2;
      return Optional.of(createOperatorToken(lexeme, tokenLine, tokenColumn));
    }

    // Then single-char operators
    String operators = "+-*/=<>";
    if (operators.indexOf(currentChar()) != -1) {
      int tokenLine = line;
      int tokenColumn = column;
      String lexeme = String.valueOf(currentChar());
      advance();
      return Optional.of(createOperatorToken(lexeme, tokenLine, tokenColumn));
    }
    return Optional.empty();
  }

  private boolean matchesTwoCharOperator() {
    if (position + 1 >= input.length()) {
      return false;
    }
    String candidate = input.substring(position, position + 2);
    return candidate.equals("==")
        || candidate.equals("!=")
        || candidate.equals("<=")
        || candidate.equals(">=");
  }

  private Token createOperatorToken(String lexeme, int line, int column) {
    return switch (lexeme) {
      case "+"  -> new PlusToken(line, column);
      case "-"  -> new MinusToken(line, column);
      case "*"  -> new StarToken(line, column);
      case "/"  -> new DivideToken(line, column);
      case "="  -> new AssignToken(line, column);
      case "==" -> new EqualsToken(line, column);
      case "!=" -> new NotEqualsToken(line, column);
      case "<"  -> new LessThanToken(line, column);
      case ">"  -> new GreaterThanToken(line, column);
      case "<=" -> new LessEqualToken(line, column);
      case ">=" -> new GreaterEqualToken(line, column);
      default   -> throw new IllegalArgumentException("Unknown operator: " + lexeme);
    };
  }

  // --------------------------------------------------------------------
  //  6) DELIMITERS (parentheses, braces, commas, etc.)
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchDelimiter() {
    // Special-case EOF marker
    if (matchesExact("EOF")) {
      int tokenLine = line;
      int tokenColumn = column;
      position += 3; // length of "EOF"
      column += 3;
      return Optional.of(new EndOfFileToken(tokenLine, tokenColumn));
    }

    // Single-character delimiters
    String delimiters = "()[]{};,.";
    if (delimiters.indexOf(currentChar()) != -1) {
      int tokenLine = line;
      int tokenColumn = column;
      char c = currentChar();
      advance();
      return Optional.of(createDelimiterToken(c, tokenLine, tokenColumn));
    }
    return Optional.empty();
  }

  private Token createDelimiterToken(char c, int line, int column) {
    return switch (c) {
      case '(' -> new LeftParenToken(line, column);
      case ')' -> new RightParenToken(line, column);
      case '{' -> new LeftBraceToken(line, column);
      case '}' -> new RightBraceToken(line, column);
      case ';' -> new SemicolonToken(line, column);
      case ',' -> new CommaToken(line, column);
      case '.' -> new DotToken(line, column);
      default  -> throw new IllegalArgumentException("Unknown delimiter: " + c);
    };
  }

  // --------------------------------------------------------------------
  //  Helper: checks if the upcoming substring matches `str` exactly,
  //  and is not followed by a letter/digit.
  // --------------------------------------------------------------------

  private boolean matchesExact(String str) {
    if (position + str.length() > input.length()) {
      return false;
    }
    String substring = input.substring(position, position + str.length());
    if (!substring.equals(str)) {
      return false;
    }
    // If the next character is a letter/digit, we haven't matched a "whole" word
    int nextPos = position + str.length();
    if (nextPos < input.length()) {
      char nextChar = input.charAt(nextPos);
      return !Character.isLetterOrDigit(nextChar);
    }
    return true;
  }
}
