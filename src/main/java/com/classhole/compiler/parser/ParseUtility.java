package com.classhole.compiler.parser;

import com.classhole.compiler.lexer.Token;
import java.text.ParseException;

public class ParseUtility {

  /**
   * Reads the token at the parser's current position and advances it.
   * If the token is not of the expected class, throws ParseException.
   */
  public static <T extends Token> T expect(Parser parser, Class<T> expectedClass, String errMsg) throws ParseException {
    Token currToken = parser.readToken(parser.getPos());
    if (!expectedClass.isInstance(currToken)) {
      throw new ParseException(errMsg + ": found " + currToken, parser.getPos());
    }
    parser.setPos(parser.getPos() + 1);
    return expectedClass.cast(currToken);
  }

}
