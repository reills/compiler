# classhole Compiler

- **Overview**
    - This project is a simple compiler
- **Project Structure**
    - `src/main/java/com/classhole/compiler/lexer` —  the tokenizer/lexer
    - `src/main/java/com/classhole/compiler/parser` — has the parser
    - `src/main/java/com/classhole/compiler/typechecker` — has the type checking
    - `src/main/java/com/classhole/compiler/codegenerator` — for code generation 
    - `src/test/java` — all the JUnit tests

- **Build & Test Commands**
    - `mvn clean compile`
        - Compiles all source files in `src/main/java`.
    - `mvn test`
        - Runs all tests under `src/test/java`.
    - mvn exec:java -Dexec.mainClass="com.classhole.compiler.Main"
        - Edit the code string in Main.java to any valid classhole program and run this to compile and print the generated JavaScript

- **Concrete Syntax**
var is a variable
classname is the name of a class
methodname is the name of a method
str is a string
i is an integer

type ::= `Int` | `Boolean` | `Void` | Built-in types
         classname class type; includes Object and String

comma_exp ::= [exp (`,` exp)*]

primary_exp ::=
  var | str | i | Variables, strings, and integers are     
                  expressions
  `(` exp `)` | Parenthesized expressions
  `this` | Refers to my instance
  `true` | `false` | Booleans
  `println` `(` exp `)` | Prints something to the terminal
  `new` classname `(` comma_exp `)` Creates a new object

call_exp ::= primary_exp (`.` methodname `(` comma_exp `)`)*

mult_exp ::= call_exp ((`*` | `/`) call_exp)*

add_exp ::= mult_exp ((`+` | `-`) mult_exp)*

exp ::= add_exp

vardec ::= type var

stmt ::= vardec `;` | Variable declaration
         var `=` exp `;` | Assignment
         `while` `(` exp `)` stmt | while loops
         `break` `;` | break
         `return` [exp] `;` | return, possibly void
         if with optional else
         `if` `(` exp `)` stmt [`else` stmt] | 
         `{` stmt* `}` Block

comma_vardec ::= [vardec (`,` vardec)*]

methoddef ::= `method` methodname `(` comma_vardec `)` type
              `{` stmt* `}`

constructor ::= `init` `(` comma_vardec `)` `{`
                [`super` `(` comma_exp `)` `;` ]
                stmt*
                `}`
classdef ::= `class` classname [`extends` classname] `{`
             (vardec `;`)*
             constructor
             methoddef*
             `}`

program ::= classdef* stmt+  stmt+ is the entry point


Example (animals with a speak method):

class Animal {
  init() {}
  method speak() Void { return println(0); }
}
class Cat extends Animal {
  init() { super(); }
  method speak() Void { return println(1); }
}
class Dog extends Animal {
  init() { super(); }
  method speak() Void { return println(2); }
}

Animal cat;
Animal dog;
cat = new Cat();
dog = new Dog();
cat.speak();
dog.speak();
