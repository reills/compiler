package com.classhole.compiler.codegenerator;

public class CodeUtils {
  public static String indent(String code, int level) {
    return "  ".repeat(level) + code;
  }

  public static String escapeString(String str) {
    return str.replace("\"", "\\\"");
  }
}
