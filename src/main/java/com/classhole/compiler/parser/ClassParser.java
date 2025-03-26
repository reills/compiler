package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.*;
import com.classhole.compiler.lexer.delimiters.*;
import com.classhole.compiler.lexer.keywords.*;
import com.classhole.compiler.lexer.literals.IdentifierToken;
import com.classhole.compiler.lexer.primitives.*;
import com.classhole.compiler.parser.ast.nodes.definitions.*;
import com.classhole.compiler.parser.ast.nodes.statements.*;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.Exp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassParser {

  /*
  *
  classdef ::= `class` classname [`extends` classname] `{`
      (vardec `;`)*
      constructor
      methoddef*
      `}`
  * */
  public static ParseResult<ClassDef> parseClassDef(Parser parser) throws ParseException {
    // Expect "class"
    ParseUtility.expect(parser, ClassToken.class, "Expected 'class'");

    // Class name
    Token nameTok = parser.readToken(parser.getPos());
    if (!(nameTok instanceof IdentifierToken idTok)) {
      throw new ParseException("Expected class name after 'class'", parser.getPos());
    }
    String className = idTok.name();
    parser.setPos(parser.getPos() + 1);

    // Optional "extends"
    Optional<String> superClass = Optional.empty();
    Token next = parser.peek();
    if (next instanceof ExtendsToken) {
      parser.setPos(parser.getPos() + 1);
      Token superTok = parser.readToken(parser.getPos());
      if (!(superTok instanceof IdentifierToken id)) {
        throw new ParseException("Expected superclass name after 'extends'", parser.getPos());
      }
      superClass = Optional.of(id.name());
      parser.setPos(parser.getPos() + 1);
    }

    // Expect '{'
    ParseUtility.expect(parser, LeftBraceToken.class, "Expected '{' at start of class body");

    // Field declarations
    List<VarDecStmt> fields = new ArrayList<>();
    while (true) {
      Token lookahead = parser.peek();
      if (lookahead instanceof IntTypeToken
          || lookahead instanceof BooleanTypeToken
          || lookahead instanceof VoidTypeToken
          || lookahead instanceof IdentifierToken) {

        Token typeTok = parser.readToken(parser.getPos());
        Token name = parser.readToken(parser.getPos() + 1);
        Token semi = parser.readToken(parser.getPos() + 2);

        if (name instanceof IdentifierToken && semi instanceof SemicolonToken) {
          String type = typeTok.getLexeme();
          String varName = ((IdentifierToken) name).name();
          fields.add(new VarDecStmt(type, varName));
          parser.setPos(parser.getPos() + 3);
        } else {
          break;
        }
      } else {
        break;
      }
    }

    // Constructor
    Token initTok = parser.readToken(parser.getPos());
    if (!(initTok.getLexeme().equals("init"))) {
      throw new ParseException("Expected constructor 'init'", parser.getPos());
    }
    parser.setPos(parser.getPos() + 1);

    ParseResult<ConstructorDef> constructor = parseConstructor(parser );
    parser.setPos(constructor.nextPos());

    // Method definitions
    List<MethodDef> methods = new ArrayList<>();
    while (parser.peek() instanceof MethodToken) {
      ParseResult<MethodDef> method = parseMethodDef(parser );
      methods.add(method.result());
      parser.setPos(method.nextPos());
    }

    ParseUtility.expect(parser, RightBraceToken.class, "Expected '}' at end of class");

    return new ParseResult<>(new ClassDef(className, superClass, fields, constructor.result(), methods), parser.getPos());
  }

  /*
  constructor ::= `init` `(` comma_vardec `)` `{`
     [`super` `(` comma_exp `)` `;` ]
     stmt*
     `}`
  */
  public static ParseResult<ConstructorDef> parseConstructor(Parser parser ) throws ParseException {
    // (
    ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after init");

    // comma_vardec
    ParseResult<List<VarDecStmt>> params = StatementParser.parseCommaVarDec(parser, parser.getPos());
    parser.setPos(params.nextPos());

    // )
    ParseUtility.expect(parser, RightParenToken.class, "Expected ')' after constructor params");

    // {
    ParseUtility.expect(parser, LeftBraceToken.class, "Expected '{' at start of constructor body");

    // Optional super(...)
    Optional<List<Exp>> superArgs = Optional.empty();
    Token next = parser.peek();
    if (next instanceof IdentifierToken id && id.name().equals("super")) {
      parser.setPos(parser.getPos() + 1); // consume 'super'
      ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after 'super'");
      ParseResult<List<Exp>> args = ExpressionParser.parseCommaExp(parser, parser.getPos());
      superArgs = Optional.of(args.result());
      parser.setPos(args.nextPos());
      ParseUtility.expect(parser, RightParenToken.class, "Expected ')' after super arguments");
      ParseUtility.expect(parser, SemicolonToken.class, "Expected ';' after super call");
    }

    // Body stmts
    List<Stmt> body = new ArrayList<>();
    while (!(parser.peek() instanceof RightBraceToken)) {
      ParseResult<Stmt> stmt = StatementParser.parseStmt(parser);
      body.add(stmt.result());
      parser.setPos(stmt.nextPos());
    }

    ParseUtility.expect(parser, RightBraceToken.class, "Expected '}' to close constructor");

    return new ParseResult<>(new ConstructorDef(params.result(), superArgs, body), parser.getPos());
  }

  /*
  methoddef ::= `method` methodname `(` comma_vardec `)` type
             `{` stmt* `}`
  */
  public static ParseResult<MethodDef> parseMethodDef(Parser parser ) throws ParseException {

    ParseUtility.expect(parser, MethodToken.class, "Expected 'method'");

    Token nameTok = parser.readToken(parser.getPos());
    if (!(nameTok instanceof IdentifierToken idTok)) {
      throw new ParseException("Expected method name", parser.getPos());
    }
    String methodName = idTok.name();
    parser.setPos(parser.getPos() + 1);

    // Parameters
    ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after method name");
    ParseResult<List<VarDecStmt>> params = StatementParser.parseCommaVarDec(parser, parser.getPos());
    parser.setPos(params.nextPos());
    ParseUtility.expect(parser, RightParenToken.class, "Expected ')' after parameters");

    // Return type
    Token typeTok = parser.readToken(parser.getPos());
    if (!(typeTok instanceof IntTypeToken
        || typeTok instanceof BooleanTypeToken
        || typeTok instanceof VoidTypeToken
        || typeTok instanceof IdentifierToken)) {
      throw new ParseException("Expected return type after method parameters", parser.getPos());
    }
    String returnType = typeTok.getLexeme();
    parser.setPos(parser.getPos() + 1);

    // Body
    ParseUtility.expect(parser, LeftBraceToken.class, "Expected '{' to start method body");
    List<Stmt> body = new ArrayList<>();
    while (!(parser.peek() instanceof RightBraceToken)) {
      ParseResult<Stmt> stmt = StatementParser.parseStmt(parser);
      body.add(stmt.result());
      parser.setPos(stmt.nextPos());
    }
    ParseUtility.expect(parser, RightBraceToken.class, "Expected '}' to close method");

    return new ParseResult<>(new MethodDef(methodName, params.result(), returnType, body), parser.getPos());
  }
}
