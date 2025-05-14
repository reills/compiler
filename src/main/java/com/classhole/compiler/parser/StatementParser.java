package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.*;
import com.classhole.compiler.lexer.delimiters.*;
import com.classhole.compiler.lexer.keywords.*;
import com.classhole.compiler.lexer.literals.IdentifierToken;
import com.classhole.compiler.lexer.primitives.*;
import com.classhole.compiler.lexer.operators.*;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.statements.*;
import com.classhole.compiler.parser.ast.Exp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StatementParser {

  public static ParseResult<Stmt> parseStmt(Parser parser) throws ParseException {
    int pos = parser.getPos();
    Token token = parser.peek();

    // Block: { stmt* }
    if (token instanceof LeftBraceToken) {
      parser.setPos(pos + 1); // consume '{'
      List<Stmt> stmts = new ArrayList<>();
      while (!(parser.peek() instanceof RightBraceToken)) {
        ParseResult<Stmt> stmt = parseStmt(parser);
        stmts.add(stmt.result());
        parser.setPos(stmt.nextPos());
      }
      parser.setPos(parser.getPos() + 1); // consume '}'
      return new ParseResult<>(new BlockStmt(stmts), parser.getPos());
    }

    // While loop
    if (token instanceof WhileToken) {
      parser.setPos(pos + 1);
      ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after 'while'");
      ParseResult<Exp> cond = ExpressionParser.exp(parser, parser.getPos());
      parser.setPos(cond.nextPos());
      ParseUtility.expect(parser, RightParenToken.class, "Expected ')' after while condition");
      ParseResult<Stmt> body = parseStmt(parser);
      return new ParseResult<>(new WhileStmt(cond.result(), body.result()), body.nextPos());
    }

    // If/else statement
    if (token instanceof IfToken) {
      parser.setPos(pos + 1);
      ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after 'if'");
      ParseResult<Exp> cond = ExpressionParser.exp(parser, parser.getPos());
      parser.setPos(cond.nextPos());
      ParseUtility.expect(parser, RightParenToken.class, "Expected ')' after if condition");
      ParseResult<Stmt> thenBranch = parseStmt(parser);
      Optional<Stmt> elseBranch = Optional.empty();

      if (parser.peek() instanceof ElseToken) {
        parser.setPos(parser.getPos() + 1);
        ParseResult<Stmt> elseStmt = parseStmt(parser);
        elseBranch = Optional.of(elseStmt.result());
        parser.setPos(elseStmt.nextPos());
      }

      return new ParseResult<>(new IfStmt(cond.result(), thenBranch.result(), elseBranch), parser.getPos());
    }

    // Return statement
    if (token instanceof ReturnToken) {
      parser.setPos(pos + 1);
      Token next = parser.peek();
      if (next instanceof SemicolonToken) {
        parser.setPos(parser.getPos() + 1);
        return new ParseResult<>(new ReturnStmt(null), parser.getPos());
      }
      ParseResult<Exp> expr = ExpressionParser.exp(parser, parser.getPos());
      parser.setPos(expr.nextPos());
      ParseUtility.expect(parser, SemicolonToken.class, "Expected ';' after return value");
      return new ParseResult<>(new ReturnStmt(Optional.of(expr.result())), parser.getPos());
    }

    // Break statement
    if (token instanceof BreakToken) {
      parser.setPos(pos + 1);
      ParseUtility.expect(parser, SemicolonToken.class, "Expected ';' after 'break'");
      return new ParseResult<>(new BreakStmt(), parser.getPos());
    }

    // Variable declaration: type var ;
    if (token instanceof IntTypeToken
        || token instanceof BooleanTypeToken
        || token instanceof VoidTypeToken
        || token instanceof IdentifierToken) {

      Token next = parser.readToken(pos + 1);
      Token nextNext = parser.readToken(pos + 2);
      if (next instanceof IdentifierToken && nextNext instanceof SemicolonToken) {
        Token typeTok = parser.readToken(pos);
        String type = typeTok.getLexeme();
        Token varTok = parser.readToken(pos + 1);
        String var = ((IdentifierToken) varTok).name();
        parser.setPos(pos + 3); // consume type, name, semicolon
        return new ParseResult<>(new VarDecStmt(type, var), parser.getPos());
      }
    }

    // Assignment: var = exp ;
    if (token instanceof IdentifierToken idTok) {
      Token next = parser.readToken(pos + 1);
      if (next instanceof AssignToken) {
        parser.setPos(pos + 2);
        ParseResult<Exp> expr = ExpressionParser.exp(parser, parser.getPos());
        parser.setPos(expr.nextPos());
        ParseUtility.expect(parser, SemicolonToken.class, "Expected ';' after assignment");
        return new ParseResult<>(new AssignStmt(idTok.name(), expr.result()), parser.getPos());
      }
    }

    // super(...) ;
    if (token instanceof SuperToken) {
      parser.setPos(parser.getPos() + 1); // consume 'super'

      ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after 'super'");

      List<Exp> args = new ArrayList<>();
      if (!(parser.peek() instanceof RightParenToken)) {
        while (true) {
          ParseResult<Exp> arg = ExpressionParser.exp(parser, parser.getPos());
          args.add(arg.result());
          parser.setPos(arg.nextPos());

          Token next = parser.peek();
          if (next instanceof CommaToken) {
            parser.setPos(parser.getPos() + 1);
          } else if (next instanceof RightParenToken) {
            break;
          } else {
            throw new ParseException("Expected ',' or ')' in super(...) argument list", parser.getPos());
          }
        }
      }
      parser.setPos(parser.getPos() + 1); // consume ')'
      ParseUtility.expect(parser, SemicolonToken.class, "Expected ';' after super(...)");
      return new ParseResult<>(new SuperStmt(args), parser.getPos());
    }

    // Expression statement: exp ;
    try {
      ParseResult<Exp> expr = ExpressionParser.exp(parser, parser.getPos());
      parser.setPos(expr.nextPos());

      ParseUtility.expect(parser, SemicolonToken.class, "Expected ';' after expression statement");

      return new ParseResult<>(new ExprStmt(expr.result()), parser.getPos());
    } catch (ParseException ignored) {
      // fall through to the error below
    }

    throw new ParseException("Unexpected token at start of statement: " + token, pos);
  }

  public static ParseResult<VarDecStmt> parseSingleVarDec(Parser parser, int startPos) throws ParseException {
    int pos = startPos;
    Token typeTok = parser.readToken(pos++);
    String type;

    if (typeTok instanceof IntTypeToken
        || typeTok instanceof BooleanTypeToken
        || typeTok instanceof VoidTypeToken
        || typeTok instanceof IdentifierToken) {
      type = typeTok.getLexeme();
    } else {
      throw new ParseException("Expected type in variable declaration", pos - 1);
    }

    Token nameTok = parser.readToken(pos++);
    if (!(nameTok instanceof IdentifierToken)) {
      throw new ParseException("Expected variable name after type", pos - 1);
    }

    VarDecStmt varDec = new VarDecStmt(type, ((IdentifierToken) nameTok).name());
    return new ParseResult<>(varDec, pos);
  }

  public static ParseResult<List<VarDecStmt>> parseCommaVarDec(Parser parser, int startPos) throws ParseException {
    List<VarDecStmt> vars = new ArrayList<>();
    int pos = startPos;

    if (parser.readToken(pos) instanceof RightParenToken) {
      return new ParseResult<>(vars, pos);
    }

    ParseResult<VarDecStmt> first = parseSingleVarDec(parser, pos);
    vars.add(first.result());
    pos = first.nextPos();

    while (parser.readToken(pos) instanceof CommaToken) {
      pos++;
      ParseResult<VarDecStmt> nextVar = parseSingleVarDec(parser, pos);
      vars.add(nextVar.result());
      pos = nextVar.nextPos();
    }

    return new ParseResult<>(vars, pos);
  }
}
