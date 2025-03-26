package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.keywords.ClassToken;
import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.definitions.ClassDef;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class Parser {

  public final Token[] tokens;
  private int pos;

  public Parser(final Token[] tokens) {
    this.tokens = tokens;
    this.pos = 0;
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public Token readToken(final int pos) throws ParseException {
    if (pos < 0 || pos >= tokens.length) {
      throw new ParseException("Ran out of tokens", pos);
    } else {
      return tokens[pos];
    }
  }

  public Token peek() {
    return pos < tokens.length ? tokens[pos] : null;
  }

  private boolean eof() {
    return pos >= tokens.length;
  }

  // Entrypoint for the entire program
  public Program parseWholeProgram() throws ParseException {
    List<ClassDef> classes = new ArrayList<>();
    List<Stmt> entryPointStmts = new ArrayList<>();

    // Parse class definitions
    while (!eof() && peek() instanceof ClassToken) {
      ParseResult<ClassDef> classRes = ClassParser.parseClassDef(this);
      classes.add(classRes.result());
      pos = classRes.nextPos();
    }

    if (eof()) {
      throw new ParseException("Expected at least one statement after class definitions", pos);
    }

    // Parse entry point statements
    while (!eof()) {
      ParseResult<Stmt> stmtRes = StatementParser.parseStmt(this);
      entryPointStmts.add(stmtRes.result());
      pos = stmtRes.nextPos();
    }

    return new Program(classes, entryPointStmts);
  }
}
