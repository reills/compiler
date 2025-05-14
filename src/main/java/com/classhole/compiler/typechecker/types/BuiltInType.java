package com.classhole.compiler.typechecker.types;

import com.classhole.compiler.typechecker.Type;

public record BuiltInType(String name) implements Type {
  public static final BuiltInType OBJECT = new BuiltInType("Object");
  public static final BuiltInType STRING = new BuiltInType("String");

  @Override
  public String getName() {
    return name;
  }
}