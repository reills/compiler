package com.classhole.compiler.lexer.delimiters;

import com.classhole.compiler.lexer.Token;

public record LeftSquareBracketToken(int line, int column) implements Token {
<<<<<<< HEAD
  @Override
  public String getLexeme() { return "["; }
}
=======
    @Override
    public String getLexeme() { return "["; }
}
>>>>>>> 8fa9016 (left/right square bracket token)
