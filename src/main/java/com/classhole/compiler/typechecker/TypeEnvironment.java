package com.classhole.compiler.typechecker;

import java.util.HashMap;
import java.util.Map;

public class TypeEnvironment {
  public record VarInfo(Type type, boolean isInitialized) {}

  private final Map<String, VarInfo> vars = new HashMap<>();
  private final TypeEnvironment parent;

  public TypeEnvironment() {
    this.parent = null;
  }

  public TypeEnvironment(TypeEnvironment parent) {
    this.parent = parent;
  }

  public void declare(String name, Type type) {
    if (vars.containsKey(name)) {
      throw new RuntimeException("Variable already declared: " + name);
    }
    vars.put(name, new VarInfo(type, false));
  }

  public void initialize(String name) {
    VarInfo info = lookupLocal(name);
    if (info == null) {
      throw new RuntimeException("Variable not declared: " + name);
    }
    vars.put(name, new VarInfo(info.type(), true));
  }

  public VarInfo lookup(String name) {
    VarInfo info = vars.get(name);
    if (info != null) return info;
    if (parent != null) return parent.lookup(name);
    return null;
  }

  public boolean isInitialized(String name) {
    VarInfo info = lookup(name);
    if (info == null) throw new RuntimeException("Variable not declared: " + name);
    return info.isInitialized();
  }

  private VarInfo lookupLocal(String name) {
    return vars.get(name);
  }
}
