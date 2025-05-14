package com.classhole.compiler.codegenerator;

import com.classhole.compiler.parser.ast.Program;
import com.classhole.compiler.parser.ast.Stmt;
import com.classhole.compiler.parser.ast.nodes.definitions.ClassDef;
import com.classhole.compiler.parser.ast.nodes.definitions.ConstructorDef;
import com.classhole.compiler.parser.ast.nodes.definitions.MethodDef;
import com.classhole.compiler.parser.ast.nodes.statements.*;
import com.classhole.compiler.parser.ast.nodes.expressions.*;

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
    assertTrue(js.contains("A.call(this, 123);"));// super call
    assertTrue(js.contains("B.prototype = Object.create(A.prototype);"));
    assertTrue(js.contains("a = new B()"));
    assertTrue(js.contains("a.get()"));
  }

    @Test
    public void testGenerateVarExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        VarExp exp = new VarExp("myVariable");
        assertEquals("myVariable", generator.generateExp(exp));

        // Test this.variable syntax
        VarExp thisExp = new VarExp("this.field");
        assertEquals("this.field", generator.generateExp(thisExp));
    }

    @Test
    public void testGenerateIntLiteralExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        IntLiteralExp exp = new IntLiteralExp(42);
        assertEquals("42", generator.generateExp(exp));

        IntLiteralExp negExp = new IntLiteralExp(-123);
        assertEquals("-123", generator.generateExp(negExp));
    }

    @Test
    public void testGenerateBooleanLiteralExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        BooleanLiteralExp trueExp = new BooleanLiteralExp(true);
        assertEquals("true", generator.generateExp(trueExp));

        BooleanLiteralExp falseExp = new BooleanLiteralExp(false);
        assertEquals("false", generator.generateExp(falseExp));
    }

    @Test
    public void testGenerateStringLiteralExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        StringLiteralExp exp = new StringLiteralExp("Hello, World!");
        assertEquals("\"Hello, World!\"", generator.generateExp(exp));

        // Test with quotes inside the string
        StringLiteralExp quotesExp = new StringLiteralExp("He said \"Hello\"");
        assertEquals("\"He said \\\"Hello\\\"\"", generator.generateExp(quotesExp));
    }

    @Test
    public void testGenerateThisExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        ThisExp exp = new ThisExp();
        assertEquals("this", generator.generateExp(exp));
    }

    @Test
    public void testGenerateCallMethodExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        // Simple method call: obj.method()
        CallMethodExp simpleCall = new CallMethodExp(
                new VarExp("obj"),
                List.of(new CallMethodExp.CallLink("method", List.of()))
        );
        assertEquals("obj.method()", generator.generateExp(simpleCall));

        // Method call with arguments: obj.method(1, true)
        CallMethodExp callWithArgs = new CallMethodExp(
                new VarExp("obj"),
                List.of(new CallMethodExp.CallLink("method", List.of(
                        new IntLiteralExp(1),
                        new BooleanLiteralExp(true)
                )))
        );
        assertEquals("obj.method(1, true)", generator.generateExp(callWithArgs));

        // Chained method calls: obj.method1().method2("test")
        CallMethodExp chainedCall = new CallMethodExp(
                new VarExp("obj"),
                List.of(
                        new CallMethodExp.CallLink("method1", List.of()),
                        new CallMethodExp.CallLink("method2", List.of(
                                new StringLiteralExp("test")
                        ))
                )
        );
        assertEquals("obj.method1().method2(\"test\")", generator.generateExp(chainedCall));

        // This reference call: this.method()
        CallMethodExp thisCall = new CallMethodExp(
                new ThisExp(),
                List.of(new CallMethodExp.CallLink("method", List.of()))
        );
        assertEquals("this.method()", generator.generateExp(thisCall));
    }

    @Test
    public void testGenerateBinaryExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        // Addition
        BinaryExp addExp = new BinaryExp(
                new IntLiteralExp(5),
                "+",
                new IntLiteralExp(3)
        );
        assertEquals("(5 + 3)", generator.generateExp(addExp));

        // Subtraction
        BinaryExp subExp = new BinaryExp(
                new VarExp("x"),
                "-",
                new IntLiteralExp(1)
        );
        assertEquals("(x - 1)", generator.generateExp(subExp));

        // Multiplication
        BinaryExp mulExp = new BinaryExp(
                new IntLiteralExp(4),
                "*",
                new VarExp("y")
        );
        assertEquals("(4 * y)", generator.generateExp(mulExp));

        // Division
        BinaryExp divExp = new BinaryExp(
                new VarExp("total"),
                "/",
                new IntLiteralExp(2)
        );
        assertEquals("(total / 2)", generator.generateExp(divExp));

        // Comparison
        BinaryExp compExp = new BinaryExp(
                new VarExp("a"),
                "==",
                new VarExp("b")
        );
        assertEquals("(a == b)", generator.generateExp(compExp));

        // Complex nested expression
        BinaryExp complexExp = new BinaryExp(
                new BinaryExp(
                        new VarExp("x"),
                        "+",
                        new IntLiteralExp(1)
                ),
                "*",
                new BinaryExp(
                        new VarExp("y"),
                        "-",
                        new IntLiteralExp(2)
                )
        );
        assertEquals("((x + 1) * (y - 2))", generator.generateExp(complexExp));
    }

    @Test
    public void testGenerateParenExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        ParenExp exp = new ParenExp(new VarExp("x"));
        assertEquals("(x)", generator.generateExp(exp));

        ParenExp nestedExp = new ParenExp(
                new BinaryExp(
                        new VarExp("a"),
                        "+",
                        new VarExp("b")
                )
        );
        assertEquals("((a + b))", generator.generateExp(nestedExp));
    }

    @Test
    public void testGenerateNewObjectExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        // Simple constructor: new MyClass()
        NewObjectExp simpleNew = new NewObjectExp("MyClass", List.of());
        assertEquals("new MyClass()", generator.generateExp(simpleNew));

        // Constructor with arguments: new Person("John", 30)
        NewObjectExp newWithArgs = new NewObjectExp(
                "Person",
                List.of(
                        new StringLiteralExp("John"),
                        new IntLiteralExp(30)
                )
        );
        assertEquals("new Person(\"John\", 30)", generator.generateExp(newWithArgs));

        // Constructor with complex arguments: new Calculator(x + y, obj.getValue())
        NewObjectExp complexNew = new NewObjectExp(
                "Calculator",
                List.of(
                        new BinaryExp(
                                new VarExp("x"),
                                "+",
                                new VarExp("y")
                        ),
                        new CallMethodExp(
                                new VarExp("obj"),
                                List.of(new CallMethodExp.CallLink("getValue", List.of()))
                        )
                )
        );
        assertEquals("new Calculator((x + y), obj.getValue())", generator.generateExp(complexNew));
    }

    @Test
    public void testGeneratePrintlnExp() {
        ExpressionGenerator generator = new ExpressionGenerator();

        // Simple println: println(42)
        PrintlnExp intPrintln = new PrintlnExp(new IntLiteralExp(42));
        assertEquals("console.log(42)", generator.generateExp(intPrintln));

        // String println: println("Hello")
        PrintlnExp strPrintln = new PrintlnExp(new StringLiteralExp("Hello"));
        assertEquals("console.log(\"Hello\")", generator.generateExp(strPrintln));

        // Variable println: println(x)
        PrintlnExp varPrintln = new PrintlnExp(new VarExp("x"));
        assertEquals("console.log(x)", generator.generateExp(varPrintln));

        // Complex expression println: println(obj.getValue() + 1)
        PrintlnExp complexPrintln = new PrintlnExp(
                new BinaryExp(
                        new CallMethodExp(
                                new VarExp("obj"),
                                List.of(new CallMethodExp.CallLink("getValue", List.of()))
                        ),
                        "+",
                        new IntLiteralExp(1)
                )
        );
        assertEquals("console.log((obj.getValue() + 1))", generator.generateExp(complexPrintln));
    }
}
