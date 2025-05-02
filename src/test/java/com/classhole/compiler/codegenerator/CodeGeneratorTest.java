package com.classhole.compiler.codegenerator;

import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.definitions.ClassDef;
import com.classhole.compiler.parser.ast.nodes.definitions.ConstructorDef;
import com.classhole.compiler.parser.ast.nodes.definitions.MethodDef;
import com.classhole.compiler.parser.ast.nodes.expressions.CallMethodExp;
import com.classhole.compiler.parser.ast.nodes.expressions.IntLiteralExp;
import com.classhole.compiler.parser.ast.nodes.expressions.NewObjectExp;
import com.classhole.compiler.parser.ast.nodes.expressions.VarExp;
import com.classhole.compiler.parser.ast.nodes.statements.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CodeGeneratorTest {

  @Test
  public void testSimpleProgramOutput() {
    // class Animal { init() {} method speak() Void { return println(0); } }
    ClassDef animalClass = new ClassDef(
        "Animal",
        Optional.empty(),
        List.of(),
        new ConstructorDef(List.of(), Optional.empty(), List.of()),
        List.of(
            new MethodDef(
                "speak",
                List.of(),
                "Void",
                List.of(
                    new PrintStmt(new IntLiteralExp(0)),
                    new ReturnStmt(Optional.empty())
                )
            )
        )
    );

    // Animal a; a = new Animal(); a.speak();
    List<Stmt> entryPoint = List.of(
        new VarDecStmt("Animal", "a"),
        new AssignStmt("a", new NewObjectExp("Animal", List.of())),
        new ExprStmt(new CallMethodExp(
            new VarExp("a"),
            List.of(new CallMethodExp.CallLink("speak", List.of()))
        ))
    );

    Program program = new Program(List.of(animalClass), entryPoint);

    CodeGenerator generator = new CodeGenerator();
    String js = generator.generate(program);

    System.out.println(js);

    assertTrue(js.contains("function Animal("));
    assertTrue(js.contains("Animal.prototype.speak = function()"));
    assertTrue(js.contains("a = new Animal()"));
    assertTrue(js.contains("a.speak()"));
  }

  @Test
  public void testInheritanceFieldInitAndExpressionChaining() {
    // class A { Int x; init(Int x) { this.x = x; } method get() Int { return this.x; } }
    ClassDef classA = new ClassDef(
        "A",
        Optional.empty(),
        List.of(new VarDecStmt("Int", "x")),
        new ConstructorDef(
            List.of(new VarDecStmt("Int", "x")),
            Optional.empty(),
            List.of(
                new AssignStmt("x", new VarExp("x"))
            )
        ),
        List.of(
            new MethodDef(
                "get",
                List.of(),
                "Int",
                List.of(new ReturnStmt(Optional.of(new VarExp("this.x"))))
            )
        )
    );

    // class B extends A {
    //   Boolean b;
    //   init() { super(123); this.b = true; }
    //   method speak() Void { return println(this.get() + 1 * 2); }
    // }
    ClassDef classB = new ClassDef(
        "B",
        Optional.of("A"),
        List.of(new VarDecStmt("Boolean", "b")),
        new ConstructorDef(
            List.of(),
            Optional.of(List.of(new IntLiteralExp(123))),
            List.of(
                new AssignStmt("b", new VarExp("true"))
            )
        ),
        List.of(
            new MethodDef(
                "speak",
                List.of(),
                "Void",
                List.of(
                    new PrintStmt(
                        new CallMethodExp(
                            new VarExp("this"),
                            List.of(
                                new CallMethodExp.CallLink("get", List.of())
                            )
                        )
                    ),
                    new ReturnStmt(Optional.empty())
                )
            )
        )
    );

    // A a; a = new B(); a.get();
    List<Stmt> entryPoint = List.of(
        new VarDecStmt("A", "a"),
        new AssignStmt("a", new NewObjectExp("B", List.of())),
        new ExprStmt(
            new CallMethodExp(
                new VarExp("a"),
                List.of(new CallMethodExp.CallLink("get", List.of()))
            )
        )
    );

    Program program = new Program(List.of(classA, classB), entryPoint);

    CodeGenerator generator = new CodeGenerator();
    String js = generator.generate(program);
    System.out.println(js);

    // assertions to check correctness
    assertTrue(js.contains("function A("));
    assertTrue(js.contains("this.x = x;")); // assigned from constructor param
    assertTrue(js.contains("this.b = false;"));
    assertTrue(js.contains("B.call(this, 123);"));// super call
    assertTrue(js.contains("B.prototype = Object.create(A.prototype);"));
    assertTrue(js.contains("a = new B()"));
    assertTrue(js.contains("a.get()"));
  }

}
