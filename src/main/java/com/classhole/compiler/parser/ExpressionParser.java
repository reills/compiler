package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.Token;
import com.classhole.compiler.lexer.delimiters.*;
import com.classhole.compiler.lexer.keywords.NewToken;
import com.classhole.compiler.lexer.keywords.PrintlnToken;
import com.classhole.compiler.lexer.literals.*;
import com.classhole.compiler.lexer.operators.*;
import com.classhole.compiler.lexer.keywords.ThisToken;
import com.classhole.compiler.parser.ast.Exp;
import com.classhole.compiler.parser.ast.nodes.expressions.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ExpressionParser {

  //exp ::= add_exp
  // public static ParseResult<Exp> exp(Parser parser, int startPos) throws ParseException {
  //   return addExp(parser, startPos);
  // }
  //exp ::= rel_exp
  public static ParseResult<Exp> exp(Parser parser, int startPos) throws ParseException {
    return relExp(parser, startPos);
  }

  //add_exp ::= mult_exp ((`+` | `-`) mult_exp)*
  public static ParseResult<Exp> addExp(Parser parser, int startPos) throws ParseException {
    ParseResult<Exp> left = multExp(parser, startPos);
    int pos = left.nextPos();

    while (true) {
      Token op = parser.readToken(pos);
      if (op instanceof PlusToken || op instanceof MinusToken) {
        String operator = op.getLexeme();
        ParseResult<Exp> right = multExp(parser, pos + 1);
        left = new ParseResult<>(new BinaryExp(left.result(), operator, right.result()), right.nextPos());
        pos = right.nextPos();
      } else {
        break;
      }
    }

    return left;
  }

  //mult_exp ::= call_exp ((`*` | `/`) call_exp)*
  public static ParseResult<Exp> multExp(Parser parser, int startPos) throws ParseException {
    ParseResult<Exp> left = callExp(parser, startPos);
    int pos = left.nextPos();

    while (true) {
      Token op = parser.readToken(pos);
      if (op instanceof StarToken || op instanceof DivideToken) {
        String operator = op.getLexeme();
        ParseResult<Exp> right = callExp(parser, pos + 1);
        left = new ParseResult<>(new BinaryExp(left.result(), operator, right.result()), right.nextPos());
        pos = right.nextPos();
      } else {
        break;
      }
    }

    return left;
  }

  //call_exp ::= primary_exp (`.` methodname `(` comma_exp `)`)*
  public static ParseResult<Exp> callExp(Parser parser, int startPos) throws ParseException {
    ParseResult<Exp> receiverResult = primaryExp(parser, startPos);
    Exp receiver = receiverResult.result();
    int pos = receiverResult.nextPos();

    List<CallMethodExp.CallLink> chain = new ArrayList<>();

    while (parser.readToken(pos) instanceof DotToken) {
      pos++;  // consume '.'

      Token next = parser.readToken(pos);
      if (!(next instanceof IdentifierToken methodName)) {
        throw new ParseException("Expected method name after '.'", pos);
      }
      pos++;

      if (!(parser.readToken(pos) instanceof LeftParenToken)) {
        throw new ParseException("Expected '(' after method name", pos);
      }
      pos++; // consume '('

      ParseResult<List<Exp>> argsResult = parseCommaExp(parser, pos);
      pos = argsResult.nextPos();

      if (!(parser.readToken(pos) instanceof RightParenToken)) {
        throw new ParseException("Expected ')' after arguments", pos);
      }
      pos++; // consume ')'

      chain.add(new CallMethodExp.CallLink(methodName.name(), argsResult.result()));
    }

    if (chain.isEmpty()) {
      return new ParseResult<>(receiver, pos);
    } else {
      return new ParseResult<>(new CallMethodExp(receiver, chain), pos);
    }
  }


  /*
  primary_exp ::=
     var | str | i | Variables, strings, and integers are
                     expressions
     `(` exp `)` | Parenthesized expressions
     `this` | Refers to my instance
     `true` | `false` | Booleans
     `println` `(` exp `)` | Prints something to the terminal
     `new` classname `(` comma_exp `)` Creates a new object
  * */
  public static ParseResult<Exp> primaryExp(Parser parser, int startPos) throws ParseException {
    Token token = parser.readToken(startPos);

    if (token instanceof IdentifierToken id) {
      return new ParseResult<>(new VarExp(id.name()), startPos + 1);
    } else if (token instanceof IntegerLiteralToken i) {
      return new ParseResult<>(new IntLiteralExp(i.value()), startPos + 1);
    } else if (token instanceof StringLiteralToken s) {
      return new ParseResult<>(new StringLiteralExp(s.value()), startPos + 1);
    } else if (token instanceof BooleanLiteralToken b) {
      return new ParseResult<>(new BooleanLiteralExp(b.value()), startPos + 1);
    } else if (token instanceof ThisToken) {
      return new ParseResult<>(new ThisExp(), startPos + 1);
    } else if (token instanceof PrintlnToken) {
      //  Advance the parser cursor manually before expecting '('
      parser.setPos(startPos + 1); // Move past PrintlnToken

      //  Expect and consume (
      ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after println");

      // Parse the expression inside println(...)
      int afterLParen = parser.getPos();
      ParseResult<Exp> inner = exp(parser, afterLParen);
      parser.setPos(inner.nextPos());

      //  Expect and consume
      ParseUtility.expect(parser, RightParenToken.class, "Expected ')' after expression");

      return new ParseResult<>(new PrintlnExp(inner.result()), parser.getPos());
    } else if (token instanceof NewToken) {
      // Advance past 'new'
      parser.setPos(parser.getPos() + 1);

      // Expect and consume class name
      IdentifierToken classToken = ParseUtility.expect(parser, IdentifierToken.class, "Expected class name after 'new'");
      String className = classToken.name();

      // Expect and consume '('
      ParseUtility.expect(parser, LeftParenToken.class, "Expected '(' after class name");

      // Parse constructor arguments
      List<Exp> args = new ArrayList<>();
      if (!(parser.readToken(parser.getPos()) instanceof RightParenToken)) {
        while (true) {
          ParseResult<Exp> arg = exp(parser, parser.getPos());
          args.add(arg.result());
          parser.setPos(arg.nextPos());

          Token next = parser.readToken(parser.getPos());
          if (next instanceof CommaToken) {
            parser.setPos(parser.getPos() + 1); // skip comma
          } else if (next instanceof RightParenToken) {
            break;
          } else {
            throw new ParseException("Expected ',' or ')' in argument list", parser.getPos());
          }
        }
      }

      // Consume ')'
      parser.setPos(parser.getPos() + 1);

      return new ParseResult<>(new NewObjectExp(className, args), parser.getPos());
    } else if (token instanceof LeftParenToken) {
      int pos = startPos + 1;
      ParseResult<Exp> inner = exp(parser, pos);
      pos = inner.nextPos();

      Token next = parser.readToken(pos);
      if (!(next instanceof RightParenToken)) {
        throw new ParseException("Expected ')'", pos);
      }

      return new ParseResult<>(new ParenExp(inner.result()), pos + 1);
    }

    throw new ParseException("Unexpected token in primary expression: " + token, startPos);
  }

  //comma_exp ::= [exp (`,` exp)*]
  public static ParseResult<List<Exp>> parseCommaExp(Parser parser, int startPos) throws ParseException {
    List<Exp> args = new ArrayList<>();
    int pos = startPos;

    Token token = parser.readToken(pos);
    if (token instanceof RightParenToken) {
      return new ParseResult<>(args, pos); // empty list
    }

    ParseResult<Exp> first = exp(parser, pos);
    args.add(first.result());
    pos = first.nextPos();

    while (parser.readToken(pos) instanceof CommaToken) {
      pos++; // consume comma
      ParseResult<Exp> next = exp(parser, pos);
      args.add(next.result());
      pos = next.nextPos();
    }

    return new ParseResult<>(args, pos);
  }


//rel_exp ::= add_exp ((== | != | <= | >= | < | >) add_exp)*
public static ParseResult<Exp> relExp(Parser parser, int startPos) throws ParseException {
  ParseResult<Exp> left = addExp(parser, startPos);
  int pos = left.nextPos();

  while (true) {
    Token op = parser.readToken(pos);
    if (op instanceof EqualsToken || op instanceof NotEqualsToken ||
        op instanceof LessEqualToken || op instanceof GreaterEqualToken ||
        op instanceof LessThanToken || op instanceof GreaterThanToken) {

      String operator = op.getLexeme();
      ParseResult<Exp> right = addExp(parser, pos + 1);
      left = new ParseResult<>(new BinaryExp(left.result(), operator, right.result()), right.nextPos());
      pos = right.nextPos();
    } else {
      break;
    }
  }

  return left;
}



}
