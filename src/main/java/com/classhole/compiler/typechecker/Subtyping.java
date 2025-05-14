package com.classhole.compiler.typechecker;

import java.util.HashMap;
import java.util.Map;

public class Subtyping {
  private final Map<String, String> parentMap = new HashMap<>();

  /**
   * Register a subclass-superclass relationship: subclass extends superclass.
   */
  public void addSubtype(String subclass, String superclass) {
    parentMap.put(subclass, superclass);
  }

  /**
   * Checks whether `sub` is a subtype of `sup`.
   * Reflexive: sub <: sub
   * Transitive: if A <: B and B <: C then A <: C
   */
  public boolean isSubtype(String sub, String sup) {
    if (sub.equals(sup)) return true;

    String current = parentMap.get(sub);
    while (current != null) {
      if (current.equals(sup)) return true;
      current = parentMap.get(current);
    }

    return false;
  }
}
