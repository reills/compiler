package com.classhole.compiler.typechecker;

import com.classhole.compiler.parser.ast.nodes.definitions.ClassDef;
import com.classhole.compiler.parser.ast.nodes.definitions.ConstructorDef;
import com.classhole.compiler.parser.ast.nodes.definitions.MethodDef;
import com.classhole.compiler.parser.ast.nodes.statements.VarDecStmt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClassTable {
  public static class ClassInfo {
    public final String name;
    public final Optional<String> superClassName;
    public final Map<String, String> fieldTypes = new HashMap<>();
    public final Map<String, MethodDef> methods = new HashMap<>();
    public final ConstructorDef constructor;

    public ClassInfo(String name, Optional<String> superClassName,
        List<VarDecStmt> fields,
        ConstructorDef constructor,
        List<MethodDef> methodList) {
      this.name = name;
      this.superClassName = superClassName;
      this.constructor = constructor;

      // Field types
      for (VarDecStmt field : fields) {
        String varName = field.name();
        if (fieldTypes.containsKey(varName)) {
          throw new RuntimeException("Duplicate field: " + varName + " in class " + name);
        }
        fieldTypes.put(varName, field.type());
      }

      // Methods
      for (MethodDef method : methodList) {
        String methodName = method.name();
        if (methods.containsKey(methodName)) {
          throw new RuntimeException("Duplicate method: " + methodName + " in class " + name);
        }
        methods.put(methodName, method);
      }
    }
  }

  private final Map<String, ClassInfo> classes = new HashMap<>();

  public void addClass(ClassDef classDef) {
    String name = classDef.className();
    if (classes.containsKey(name)) {
      throw new RuntimeException("Class already defined: " + name);
    }

    ClassInfo info = new ClassInfo(
        name,
        classDef.superClass(),
        classDef.fields(),
        classDef.constructor(),
        classDef.methods()
    );

    classes.put(name, info);
  }

  public ClassInfo getClass(String name) {
    return classes.get(name);
  }

  public MethodDef getMethod(String className, String methodName) {
    ClassInfo current = classes.get(className);
    while (current != null) {
      if (current.methods.containsKey(methodName)) {
        return current.methods.get(methodName);
      }
      if (current.superClassName.isEmpty()) break;
      current = classes.get(current.superClassName.get());
    }
    return null;
  }


}
