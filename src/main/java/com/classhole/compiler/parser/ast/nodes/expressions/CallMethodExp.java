package com.classhole.compiler.parser.ast.nodes.expressions;

import com.classhole.compiler.parser.ast.Exp;
import java.util.List;

public record CallMethodExp(Exp receiver, List<CallLink> chain) implements Exp {

  @Override
  public String toString() {
    return "CallMethodExp[receiver=" + receiver +
        ", chain=" + chain + "]";
  }

  public record CallLink(String methodName, List<Exp> args) {
    @Override
    public String toString() {
      return "CallLink[methodName=" + methodName +
          ", args=" + args + "]";
    }
  }
}
