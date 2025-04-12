package com.classhole.compiler.typechecker.types;

import com.classhole.compiler.typechecker.Type;

public record ClassType(String name) implements Type {
  @Override
  public String getName() {
    return name;
  }
}
