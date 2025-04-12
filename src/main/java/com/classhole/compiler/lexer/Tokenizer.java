package com.classhole.compiler.lexer;

import com.classhole.compiler.lexer.keywords.*;
import com.classhole.compiler.lexer.literals.*;
import com.classhole.compiler.lexer.operators.*;
import com.classhole.compiler.lexer.primitives.*;
import com.classhole.compiler.lexer.delimiters.*;

import java.util.ArrayList;
import java.util.Optional;

public class Tokenizer {
  private final String input;
  private int position;
  private int line;
  private int column;

  public Tokenizer(String input) {
    this.input = input;
    this.position = 0;
    this.line = 1;
    this.column = 1;
  }

  public int getPosition() {
    return position;
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
    if (currentChar() == '\n') {
      //System.out.println("found a newline");
      line++;
      column = 1;  // Reset column on newline
    } else {
      column++;
    }
    position++;
  }

  public void skipWhitespace() {
    while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
      advance();
    }
  }

  public Optional<Token> nextToken() {
    skipWhitespace();

    if (position >= input.length()) {
      return Optional.empty();
    }

    // Save the starting position before matching any token
    int startLine = line;
    int startColumn = column;
    char current = input.charAt(position);

    // Try to match in a particular order
    Optional<Token> token = tryMatchKeyword(startLine, startColumn)
            .or(() -> tryMatchPrimitive(startLine, startColumn))
            .or(() -> tryMatchLiteral(startLine, startColumn))
            .or(() -> tryMatchIdentifier(startLine, startColumn))
            .or(() -> tryMatchOperator(startLine, startColumn))
            .or(() -> tryMatchDelimiter(startLine, startColumn));

    if (token.isPresent()) {
      return token;
    }

    // If we get here, we found an unexpected character
    throw new IllegalStateException("Unexpected character at line " + line + ": " + current);
  }

  // --------------------------------------------------------------------
  //  1) KEYWORDS
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchKeyword(int startLine, int startColumn) {
    String keyword = matchKeyword();
    if (keyword != null) {
      Token token = createKeywordToken(keyword, startLine, startColumn);
      position += keyword.length();
      column += keyword.length();
      return Optional.of(token);
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

  private Optional<Token> tryMatchPrimitive(int startLine, int startColumn) {
    String[] primitives = {"Int", "Boolean", "Void"};
    for (String prim : primitives) {
      if (matchesExact(prim)) {
        Token token = createPrimitiveToken(prim, startLine, startColumn);
        position += prim.length();
        column += prim.length();
        return Optional.of(token);
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
  //  3) LITERALS (e.g. integers, booleans, strings)
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchLiteral(int startLine, int startColumn) {
    // 3.1 Integers
    if (Character.isDigit(currentChar())) {
      int start = position;
      while (position < input.length() && Character.isDigit(input.charAt(position))) {
        advance();
      }

      String lexeme = input.substring(start, position);
      try {
        int value = Integer.parseInt(lexeme);  // actually parse it since integer overflow
        return Optional.of(new IntegerLiteralToken(value, startLine, startColumn));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Integer literal too large at line " + startLine + ", column " + startColumn + ": " + lexeme);
      }
    }

    // 3.2 Booleans
    if (matchesExact("true")) {
      Token token = new BooleanLiteralToken(true, startLine, startColumn);
      position += 4; // length of "true"
      column += 4;
      return Optional.of(token);
    }
    if (matchesExact("false")) {
      Token token = new BooleanLiteralToken(false, startLine, startColumn);
      position += 5; // length of "false"
      column += 5;
      return Optional.of(token);
    }

    // 3.3 Strings
    if (currentChar() == '\"') {

      advance(); // skip opening quote
      int start = position;

      // Gather characters until the next quote or EOF
      while (position < input.length() && input.charAt(position) != '\"') {
        advance();
      }

      if (position >= input.length()) {
        // Reached EOF without closing quote
        throw new IllegalStateException("Unterminated string literal at line " + startLine);
      }

      // Extract the string between the quotes
      String value = input.substring(start, position);

      // Skip the closing quote
      advance();

      return Optional.of(new StringLiteralToken(value, startLine, startColumn));
    }

    return Optional.empty();
  }

  // --------------------------------------------------------------------
  //  4) IDENTIFIERS
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchIdentifier(int startLine, int startColumn) {
    if (Character.isLetter(currentChar())) {
      int start = position;
      while (position < input.length() && Character.isLetterOrDigit(input.charAt(position))) {
        advance();
      }
      String lexeme = input.substring(start, position);
      return Optional.of(new IdentifierToken(lexeme, startLine, startColumn));
    }
    return Optional.empty();
  }

  // --------------------------------------------------------------------
  //  5) OPERATORS
  // --------------------------------------------------------------------

  private Optional<Token> tryMatchOperator(int startLine, int startColumn) {
    // Check for two-char operators first
    if (matchesTwoCharOperator()) {
      String lexeme = input.substring(position, position + 2);
      Token token = createOperatorToken(lexeme, startLine, startColumn);
      position += 2;
      column += 2;
      return Optional.of(token);
    }

    // Then single-char operators
    String operators = "+-*/=<>";
    if (operators.indexOf(currentChar()) != -1) {
      String lexeme = String.valueOf(currentChar());
      Token token = createOperatorToken(lexeme, startLine, startColumn);
      advance();
      return Optional.of(token);
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

  private Optional<Token> tryMatchDelimiter(int startLine, int startColumn) {
    // Single-character delimiters
    String delimiters = "()[]{};,.";
    if (delimiters.indexOf(currentChar()) != -1) {
      char c = currentChar();
      Token token = createDelimiterToken(c, startLine, startColumn);
      advance();
      return Optional.of(token);
    }
    return Optional.empty();
  }

  private Token createDelimiterToken(char c, int line, int column) {
    return switch (c) {
      case '(' -> new LeftParenToken(line, column);
      case ')' -> new RightParenToken(line, column);
      case '{' -> new LeftBraceToken(line, column);
      case '}' -> new RightBraceToken(line, column);
      case '[' -> new LeftSquareBracketToken(line, column);
      case ']' -> new RightSquareBracketToken(line, column);
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

  public ArrayList<Token> tokenize(){
    final ArrayList<Token> tokens = new ArrayList<>();
    skipWhitespace();
    while (position < input.length()) {
      Optional<Token> token = nextToken();
      token.ifPresent(tokens::add);
    }
    return tokens;
  }
}
