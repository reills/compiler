package com.classhole.compiler.typechecker.types;


import com.classhole.compiler.typechecker.Type;

public record PrimitiveType(String name) implements Type {
  public static final PrimitiveType INT = new PrimitiveType("Int");
  public static final PrimitiveType BOOLEAN = new PrimitiveType("Boolean");
  public static final PrimitiveType VOID = new PrimitiveType("Void");

  @Override
  public String getName() {
    return name;
  }
}
