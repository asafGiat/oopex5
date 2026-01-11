# Plan: s-Java Code Verifier Implementation

Build a Java verifier tool that validates simplified Java syntax (s-Java) using regular expressions and exception handling. The program reads an s-Java source file, preprocesses it, builds a scope tree, validates the code structure, and outputs 0 (valid), 1 (syntax error), or 2 (IO error) with descriptive error messages.

## Architecture Overview

The system follows a pipeline architecture:
1. **File Reading** → 2. **Preprocessing** → 3. **Parsing & Scope Building** → 4. **Validation** → 5. **Output**

### Package Structure
- `ex5.main` - Entry point (`Sjavac` class) + `InvalidFileException` (thrown by main)
- `ex5.preprocessor` - Line cleanup (`CodePreprocessor`) + `PreprocessorException`
- `ex5.parser` - Parsing logic (`CodeParser`) + `ParserException`
- `ex5.models` - Data models (`Variable`, `VariableTable`, `Method`, `Statement`)
- `ex5.scope` - Scope hierarchy (`Scope`, `GlobalScope`, `MethodScope`, `BlockScope`) + `ScopeException`
- `ex5.validator` - Validation logic and `RegexManager` + `VariableException`, `MethodException`, `ConditionException`

**Note**: Per requirements section 6.1, each exception class must be in the SAME package as the class that throws it, NOT in a separate exceptions package. Exception classes should NOT be nested classes.

## Steps

### 1. Design OOP Architecture & Create UML Diagram
- Define package structure with clear separation of concerns
- Design class hierarchy for `Scope` with subtypes: `GlobalScope`, `MethodScope`, `BlockScope`
- Plan models: `Variable`, `VariableTable`, `Method`, `Statement` classes
- Create UML diagram showing:
  - Package organization
  - Class relationships and inheritance hierarchy (especially `Scope` hierarchy)
  - Key methods and attributes
  - Data flow from `Sjavac` → `CodePreprocessor` → `CodeParser` → `ScopeTree` → `Validator`
  - Composition relationships (e.g., `Scope` contains `VariableTable`)

### 2. Implement Models Package (`ex5.models`)
- **`Variable` class**: 
  - Attributes: `String name`, `String type`, `boolean isFinal`, `boolean isInitialized`, `int lineNumber`, `boolean isParameter`
  - Methods: getters, setters, `initialize()`, `canAssign()` (check if final)
  - Note: Parameters are always considered initialized
- **`VariableTable` class**: 
  - Internal: `HashMap<String, Variable>` to store variables
  - Methods:
    - `addVariable(Variable v)` - Add variable with name conflict checking in current scope
    - `getVariable(String name)` - Retrieve variable by name (local scope only)
    - `isInitialized(String name)` - Check initialization status
    - `containsVariable(String name)` - Check if variable exists in this table
    - `getAllVariables()` - Return collection for iteration
- **`Method` class**: 
  - Attributes: `String name`, `List<Variable> parameters`, `MethodScope scope`, `int declarationLine`
  - Methods: `addParameter(Variable)`, `getParameterCount()`, `getParameterTypes()`, `matchesSignature()`
- **`Statement` class/enum**: 
  - Represent statement types: `VAR_DECLARATION`, `ASSIGNMENT`, `METHOD_CALL`, `IF_BLOCK`, `WHILE_BLOCK`, `RETURN`
  - Store: statement type, line number, raw content for error reporting

### 3. Implement Preprocessor (`ex5.preprocessor.CodePreprocessor`)
- Create `CodePreprocessor` class responsible for:
  - Reading file line by line
  - Removing comment lines (lines where first non-whitespace chars are `//`)
  - Detecting illegal comment formats (`/* */`, `/** */`, mid-line `//` after code)
  - Identifying empty lines (only whitespace) - track but preserve for line numbering
  - Trimming leading/trailing whitespace from code lines
  - Preserving original line numbers for error reporting
  - **Note**: Do NOT normalize internal whitespace - validation must check for required spaces:
    - Between `void` and method name (e.g., `voidfoo()` is illegal)
    - Between `final` and type (e.g., `finalint` is illegal)
    - Between type and variable name (e.g., `inta` is illegal)
- Output: `List<ProcessedLine>` where `ProcessedLine` contains:
  - `int originalLineNumber`
  - `String cleanedContent`
  - `LineType type` (EMPTY, COMMENT, CODE, INVALID)
- Validation during preprocessing:
  - Detect `/*` or `/**` anywhere → throw exception
  - Detect `//` not at start of line (after code) → throw exception

### 4. Implement RegexManager (`ex5.validator.RegexManager`)
- Create `RegexManager` class with static final `Pattern` constants:
  - **Variable & Type Patterns**:
    - `TYPE_PATTERN` - Match valid types: `(int|double|boolean|char|String)`
    - `VARIABLE_NAME` - Valid variable names: `([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9][a-zA-Z0-9_]*)` (letter+any OR underscore+letter/digit+any, but NOT __, NOT starting with digit)
    - `METHOD_NAME` - Valid method names: `[a-zA-Z][a-zA-Z0-9_]*` (MUST start with letter, not underscore)
    - `VARIABLE_DECLARATION` - Match: `(final\s+)?TYPE\s+IDENTIFIER(\s*=\s*VALUE)?(\s*,\s*IDENTIFIER(\s*=\s*VALUE)?)*\s*;`
    - `VARIABLE_ASSIGNMENT` - Match: `IDENTIFIER(\s*=\s*VALUE)(\s*,\s*IDENTIFIER\s*=\s*VALUE)*\s*;` (Note: each variable must have its own value)
  - **Value Patterns**:
    - `INT_VALUE` - `[+-]?\d+`
    - `DOUBLE_VALUE` - `[+-]?(\d+\.\d*|\d*\.\d+|\d+\.|\.\d+)`
    - `BOOLEAN_VALUE` - `true|false|INT_VALUE|DOUBLE_VALUE`
    - `CHAR_VALUE` - `'[^'\\]'`
    - `STRING_VALUE` - `"[^"\\]*"`
  - **Method Patterns**:
    - `METHOD_DECLARATION` - `void\s+METHOD_NAME\s*\(PARAMS?\)\s*\{`
    - `METHOD_CALL` - `METHOD_NAME\s*\(ARGS?\)\s*;`
  - **Control Flow Patterns**:
    - `IF_STATEMENT` - `if\s*\(CONDITION\)\s*\{`
    - `WHILE_STATEMENT` - `while\s*\(CONDITION\)\s*\{`
    - `CONDITION_PATTERN` - Boolean expressions with `&&` and `||` operators
    - `RETURN_STATEMENT` - `return\s*;`
  - **Structure Patterns**:
    - `OPEN_BRACE` - Line ending with `{`
    - `CLOSE_BRACE` - Line with only `}`
- Include utility methods:
  - `static boolean matchesPattern(String line, Pattern pattern)`
  - `static Matcher getMatcher(String line, Pattern pattern)`
  - `static String[] extractGroups(String line, Pattern pattern)`

### 5. Implement Scope Hierarchy (`ex5.scope`)
- **Base `Scope` class** (abstract):
  - Attributes:
    - `VariableTable variables` - Variables declared in this scope
    - `Scope parentScope` - Reference to parent (null for GlobalScope)
    - `List<Scope> childScopes` - Nested scopes (methods/blocks)
    - `List<Statement> statements` - Statements in this scope
    - `int startLine`, `int endLine` - Line range
  - Methods:
    - `addVariable(Variable v)` - Add to local variable table
    - `findVariable(String name)` - Search this scope, then recursively search parent
    - `addStatement(Statement s)` - Add statement to list
    - `addChildScope(Scope child)` - Link child scope
    - `abstract void validate()` - Validate this scope (implemented by subclasses)
    - `getDepth()` - Calculate nesting depth
- **`GlobalScope extends Scope`**:
  - Additional attributes:
    - `Map<String, Method> methods` - All method definitions in file
  - Additional methods:
    - `addMethod(Method m)` - Add method with name uniqueness check
    - `getMethod(String name)` - Retrieve method definition
    - `methodExists(String name)` - Check if method is defined
    - `validate()` - Validate all global variables, then validate each method
- **`MethodScope extends Scope`**:
  - Additional attributes:
    - `Method methodDefinition` - Reference to Method object
    - `boolean hasReturn` - Track if return statement found
    - `int returnLine` - Line number of return statement
  - Additional methods:
    - `addParameter(Variable param)` - Add parameter to variable table
    - `setReturn(int line)` - Mark return statement
    - `validateReturn()` - Ensure return exists and is last statement (ignore empty lines)
    - `validate()` - Validate method body, parameters, return, and nested scopes
- **`BlockScope extends Scope`** (for if/while):
  - Additional attributes:
    - `String blockType` - "if" or "while"
    - `String condition` - Condition expression
  - Additional methods:
    - `validateCondition()` - Validate condition syntax and variable references
    - `validate()` - Validate condition, then validate block contents and nested scopes

### 6. Implement Parser (`ex5.parser.CodeParser`)
- Create `CodeParser` class to build scope tree from preprocessed lines:
  - **Phase 1: First Pass - Build Global Scope**
    - Create `GlobalScope` object
    - Iterate through all preprocessed lines
    - Identify global variable declarations using `RegexManager.VARIABLE_DECLARATION`
    - Identify method signatures using `RegexManager.METHOD_DECLARATION`
    - For methods: create `Method` object, add to GlobalScope, track brace positions
    - For global variables: parse and add to GlobalScope's VariableTable
    - Track opening/closing braces to identify method body boundaries
  - **Phase 2: Parse Each Method Body**
    - For each identified method:
      - Create `MethodScope` and link to GlobalScope
      - Parse method parameters, create `Variable` objects, add to MethodScope's VariableTable
      - Iterate through method body lines:
        - **Variable declaration**: Parse with regex, add to current scope's VariableTable
        - **Assignment**: Create assignment Statement, validate later
        - **Method call**: Create method call Statement, validate later
        - **if/while**: Create `BlockScope`, recursively parse block contents, link to parent
        - **return**: Mark in MethodScope, verify syntax
        - **Closing brace `}`**: Pop scope stack (return to parent scope)
      - Maintain scope stack for nested blocks
  - **Phase 3: Finalize Scope Tree**
    - Verify all braces are balanced
    - Ensure all methods have closing braces
    - Return root `GlobalScope` with complete tree structure
- Use `RegexManager` patterns throughout for line type identification
- Throw `SyntaxException` with line number for parsing errors

### 7. Implement Validators (`ex5.validator`)
- **`GlobalValidator`**:
  - `validateGlobalVariables(GlobalScope)` - Check syntax, naming rules, no duplicates
  - `validateMethodSignatures(GlobalScope)` - Check naming, parameters, no duplicates/overloading
    - Method names MUST start with a letter (not underscore or digit)
    - Methods and variables CAN have the same name (this is allowed)
  - `validateNoMethodCallsInGlobal(GlobalScope)` - Ensure no method calls in global scope
- **`VariableValidator`**:
  - `validateVariableName(String name)` - Check naming rules (no leading digit, no `__`, etc.)
  - `validateTypeCompatibility(String fromType, String toType)` - Check assignments (int→double, int/double→boolean allowed)
  - `validateInitializationBeforeUse(Scope, Statement)` - Check variable initialized before use in expressions
    - **Critical Rule from 5.2.1**: Local variable not initialized at declaration can only be used if:
      1. Next line where it appears is an assignment to it, OR
      2. It is never used
    - **Critical Rule from 5.2.3**: When inside a method:
      - Global variables initialized anywhere in the method are considered initialized for rest of method
      - Local variables initialized in nested scope are considered initialized in parent scope for subsequent lines
      - You do NOT need to check if initialization is reachable (e.g., inside `if(false)` is OK)
  - `validateFinalVariable(Variable)` - Check final variables initialized at declaration, not reassigned
  - `validateScopeAccess(Scope, String varName)` - Verify variable accessible from current scope
    - **Critical from 5.2.3**: You CANNOT access a variable defined in a more internal scope
    - Can access: (1) local var in current or outer scope, (2) global var if in any method, (3) global var in global scope after definition line
- **`MethodValidator`**:
  - `validateMethodCall(MethodScope, String methodName, List<String> args)` - Check method exists, arg count/types match
  - `validateReturnStatement(MethodScope)` - Verify return exists and is last statement (ignoring empty lines)
    - **Critical**: Return must be last code statement (empty lines/whitespace after are OK)
    - **Note**: You do NOT need to check for unreachable code after previous return statements
  - `validateParameters(Method)` - Check parameter syntax, types, names
  - `validateFinalParameters(MethodScope)` - Ensure final params not reassigned in method body
- **`ControlFlowValidator`**:
  - `validateCondition(BlockScope)` - Parse condition, check boolean expression syntax
  - `validateConditionVariables(BlockScope, String condition)` - Verify variables in condition exist and are initialized
  - `validateOperators(String condition)` - Check `&&` and `||` placement (between expressions, not at start/end)
  - `validateBraceMatching(BlockScope)` - Ensure proper `{` and `}` structure
- **`ScopeTreeValidator`** (main coordinator):
  - `validate(GlobalScope root)` - Entry point for validation
  - Traverse scope tree depth-first:
    1. Validate GlobalScope (global vars, method signatures)
    2. For each MethodScope: validate parameters, body, return
    3. For each BlockScope: validate condition, contents
    4. Recursively validate all child scopes
  - On first error, throw appropriate exception and stop validation
  - Coordinate calls to specific validators

### 8. Implement Exception Hierarchy (distributed across packages)
**Important**: Per requirements 6.1, each exception must be in the same package as the class that throws it.

- **Base `SJavaException extends Exception`** (in `ex5.main`):
  - Attributes: `String message`, `int lineNumber`
  - Constructor: `SJavaException(String message, int lineNumber)`
  - Method: `getFormattedMessage()` - Return "Error at line X: message"
  
- **`InvalidFileException extends SJavaException`** (in `ex5.main`, thrown by `Sjavac`):
  - For file not found, wrong extension (.sjava), IO errors, illegal number of arguments
  - Exit code: 2
  - Constructor: `InvalidFileException(String message)` (no line number needed)
  
- **`SyntaxException extends SJavaException`** (in `ex5.main`, base for code errors):
  - For syntax/semantic errors in s-Java code
  - Exit code: 1
  - Constructor: `SyntaxException(String message, int lineNumber)`
  
- **Package-specific exceptions** (all extend `SyntaxException`):
  - `PreprocessorException` (in `ex5.preprocessor`) - Illegal comments detected
  - `ParserException` (in `ex5.parser`) - Parsing errors, brace mismatches
  - `ScopeException` (in `ex5.scope`) - Scope structure errors
  - `VariableException` (in `ex5.validator`) - Variable naming, initialization, type errors
  - `MethodException` (in `ex5.validator`) - Method declaration, call, return errors
  - `ConditionException` (in `ex5.validator`) - if/while condition errors

### 9. Implement Main Entry Point (`ex5.main.Sjavac`)
- **`main(String[] args)` method structure**:
  ```java
  public static void main(String[] args) {
      try {
          // 1. Validate command-line arguments
          if (args.length != 1) {
              throw new InvalidFileException("Usage: java ex5.main.Sjavac <file.sjava>");
          }
          String filePath = args[0];
          if (!filePath.endsWith(".sjava")) {
              throw new InvalidFileException("File must have .sjava extension");
          }
          
          // 2. Read and preprocess file
          CodePreprocessor preprocessor = new CodePreprocessor();
          List<ProcessedLine> lines = preprocessor.preprocess(filePath);
          
          // 3. Parse and build scope tree
          CodeParser parser = new CodeParser();
          GlobalScope globalScope = parser.parse(lines);
          
          // 4. Validate scope tree
          ScopeTreeValidator validator = new ScopeTreeValidator();
          validator.validate(globalScope);
          
          // 5. Success - code is valid
          System.out.println(0);
          
      } catch (InvalidFileException e) {
          System.err.println(e.getFormattedMessage());
          System.out.println(2);
      } catch (SyntaxException e) {
          System.err.println(e.getFormattedMessage());
          System.out.println(1);
      } catch (IOException e) {
          System.err.println("IO Error: " + e.getMessage());
          System.out.println(2);
      }
  }
  ```
- File reading: Use `BufferedReader` with try-with-resources in CodePreprocessor
- Coordinate entire validation pipeline
- Proper exception handling with specific catches

### 10. Create README & UML Documentation
- **README structure**:
  - Line 1: CS username(s) separated by comma if pair
  - Line 2: Student ID(s) in same format
  - Line 3: Empty line
  - Lines 4+:
    - **Two Main Regular Expressions**:
      1. Variable Declaration Regex - Explain capture groups for: final modifier, type, identifier, optional initialization, comma-separated variables
      2. Method Declaration/Call Regex - Explain: void keyword, method name pattern, parameter list structure
    - **Design Choices**:
      - Scope tree approach: Why building complete tree before validation enables forward method references
      - Preprocessor rationale: Cleaning code upfront simplifies regex patterns
      - RegexManager centralization: Benefits of static pattern constants
      - VariableTable per scope: How parent chain lookup works
    - **Module Responsibilities**:
      - `ex5.main`: Program entry, coordination, base exception hierarchy (`SJavaException`, `InvalidFileException`, `SyntaxException`)
      - `ex5.preprocessor`: Line cleaning, comment removal, illegal format detection + `PreprocessorException`
      - `ex5.parser`: Scope tree construction, line type identification + `ParserException`
      - `ex5.models`: Data structures for variables, methods, statements
      - `ex5.scope`: Scope hierarchy, variable lookup chain, tree structure + `ScopeException`
      - `ex5.validator`: Validation logic, regex patterns, rule enforcement + validation exceptions (`VariableException`, `MethodException`, `ConditionException`)
- **UML Diagram** (`UML.pdf`):
  - Show all packages as containers
  - Class diagrams for each package
  - Highlight inheritance: `Scope` → `GlobalScope`, `MethodScope`, `BlockScope`
  - Show composition: `Scope` ◆→ `VariableTable`, `GlobalScope` ◆→ `Map<String, Method>`
  - Show associations: `Variable` ← `VariableTable`, `Method` → `MethodScope`
  - Indicate data flow arrows: `Sjavac` → `CodePreprocessor` → `CodeParser` → `ScopeTreeValidator`
  - Key methods in each class

### 11. Testing & Refinement
- **Compilation**:
  - Compile with: `javac -Xlint:rawtypes -Xlint:empty -Xlint:divzero -Xlint:deprecation *.java`
  - Fix all warnings and errors
- **Javadoc**:
  - Add `/** */` comments to all public classes and methods
  - Generate javadoc: `javadoc -d doc ex5/**/*.java`
  - Verify no javadoc errors
- **Testing Strategy**:
  - Test with provided presubmission tests
  - Create additional test cases:
    - Deeply nested scopes (5+ levels)
    - Variable shadowing in nested blocks
    - Final variable violations (no init, reassignment)
    - Method calls with wrong arg count/types
    - Uninitialized variable usage
    - Global variable initialization in methods
    - Whitespace edge cases (tabs, multiple spaces)
    - Comment edge cases
    - Return statement validation (not last, multiple returns)
- **Debugging**:
  - Add line number tracking throughout for precise error reporting
  - Test edge cases from requirements (e.g., `_a` valid, `__a` invalid)

## Critical Edge Cases & Tricky Requirements

### Variable Naming Rules (Section 5.2)
- `_a`, `_0`, `_ab` are VALID (underscore + letter/digit + anything)
- `_`, `__`, `__a`, `___b` are INVALID (single underscore alone, or starting with two+ underscores)
- `2g`, `54_a` are INVALID (cannot start with digit)
- Variables CAN start with underscore, methods CANNOT

### Variable Assignment Syntax (Section 5.2.2)
- `a = 1, b = 2;` is VALID (multiple assignments)
- `a, b = 7;` is INVALID (cannot assign same value to multiple vars)
- `a, b = 1, 2;` is INVALID (not supported syntax)

### Variable Initialization Rules (Section 5.2.1 & 5.2.3)
- Local variable declared without init can only be used if next appearance is assignment
- Global variable initialized inside a method is considered initialized for rest of that method
- Variable initialized in nested scope (e.g., inside if) is considered initialized in outer scope after that line
- You do NOT check if initialization is reachable (inside `if(false)` counts as initialized)

### Scope and Shadowing (Section 5.2.1)
- Two local variables with same name cannot be in same block
- Two local variables with same name CAN be in different blocks, even if nested
- Local can shadow global
- Local can shadow parameter (different blocks)
- Method parameters are local variables of that method

### Method Requirements (Section 5.3)
- Method names MUST start with letter (not underscore)
- Return statement MUST exist and be last code statement (empty lines after are OK)
- Methods can call themselves recursively
- Methods can be defined in any order (forward references allowed)
- Method calls ONLY allowed inside methods, NOT in global scope

### Comments and Whitespace (Section 5.1 & 5.5)
- Only `//` comments allowed (at start of line, no chars before it)
- `/* */`, `/** */`, mid-line `//` are all ILLEGAL
- Whitespace REQUIRED between: `void` and method name, `final` and type, type and variable name
- Whitespace OPTIONAL around: parentheses, `=`, `;`, `{`, `}`

### Type Compatibility (Section 5.2.2)
- `int` → `double` is allowed
- `int` and `double` → `boolean` is allowed
- `boolean` can accept: `true`, `false`, any int value, any double value

### Control Flow Conditions (Section 5.4)
- Conditions can be: `true`, `false`, boolean/int/double variables, int/double constants
- Multiple conditions with `&&` and `||` allowed
- Operators must be BETWEEN expressions (not at start/end): `if(|| a)` is illegal
- Brackets in conditions NOT supported: `if ((a || b) && c)` is illegal

## Further Considerations

### 1. Preprocessor Scope
**Question**: Should preprocessor normalize all whitespace or preserve some structure?
**Recommendation**: Trim leading/trailing whitespace only. Let regex patterns in validation handle internal whitespace using `\s*` and `\s+` appropriately. This keeps preprocessor simple and validation flexible.

### 2. Scope Tree vs. Two-Pass Parsing
**Question**: Build complete scope tree first, then validate? Or validate during parsing?
**Recommendation**: Build complete tree first, then validate. This approach:
- Allows forward method references (methods can be called before they're defined)
- Separates parsing concerns from validation logic
- Makes code more maintainable and testable
- Enables better error messages (can reference method signatures during validation)

### 3. Statement Object Necessity
**Question**: Do we need explicit `Statement` objects, or just validate during parsing?
**Recommendation**: Use lightweight `Statement` objects storing type and line number. Benefits:
- Helps validate statement order (e.g., return is last)
- Improves error reporting with precise line numbers
- Allows two-pass validation (structure first, semantics second)
- Not over-engineered - just type enum + line number

### 4. Variable Table Implementation
**Question**: Use `HashMap<String, Variable>` or more complex structure?
**Recommendation**: Use `HashMap<String, Variable>` for simplicity and O(1) lookup. No need for `LinkedHashMap` since declaration order doesn't affect validation (except initialization before use, which is tracked by line number in Variable objects).

### 5. Method Parameter Handling
**Question**: Store parameters separately or in method's VariableTable?
**Recommendation**: Add parameters to `MethodScope`'s `VariableTable` immediately when parsing method signature. Treat them as local variables with special handling:
- Mark as initialized by default (parameters always have values when method called)
- Track if parameter is final (can't be reassigned in method body)
- No special data structure needed - flag in `Variable` object is sufficient

### 6. Regex Complexity Trade-offs
**Question**: One comprehensive regex per line type vs. multiple simpler patterns?
**Recommendation**: Use moderately complex patterns that match entire line structure, then extract parts with capture groups. Example:
- Variable declaration: One pattern matching `final? type name (= value)? (, name (= value)?)* ;`
- Then parse captured groups to extract individual variables
- Avoid splitting into too many tiny patterns (harder to coordinate)
- Avoid one giant pattern for all line types (impossible to maintain)

### 7. Error Message Detail Level
**Question**: How detailed should error messages be?
**Recommendation**: Provide context but keep concise:
- Include line number always
- Describe what's wrong: "Variable 'x' used before initialization"
- Optionally hint at fix: "Declare and initialize variable before use"
- Don't dump regex patterns or technical details in user-facing errors

## Key Requirements Summary

### Input/Output
- Single parameter: s-Java source file path
- Output: Single digit (0=legal, 1=illegal, 2=IO error) via `System.out.println()`
- Error message to `System.err` describing the problem

### s-Java Syntax Rules
- No classes, imports, or multi-file interaction
- Global variables and methods (like static members in Java)
- Supported types: int, double, boolean, char, String
- Line endings: `;` (statements), `{` (open blocks), `}` (close blocks)
- Comments: Only `//` style, no `/**/` or `/** */`
- No operators, arrays, or multi-line statements

### Variable Validation
- **Names**: 
  - Variables: letters/digits/underscore, NO leading digits, NO leading double underscore (__ is illegal)
  - Can start with single underscore if followed by letter/digit: `_a` valid, `_` and `__a` invalid
  - Methods: MUST start with letter (not underscore, not digit)
- **Declaration**: with or without initialization, comma-separated allowed with mixed initialization
- **Assignment**: Multiple assignments allowed in one line: `a = 1, b = 2;` (each must have value)
  - NOT allowed: `a, b = 7;` or `a, b = 1, 2;`
- **Final modifier**: must initialize at declaration, cannot reassign later
- **Scope rules**: 
  - Global uniqueness (no two globals with same name)
  - Local shadowing allowed (local can shadow global or outer scope local)
  - Initialization before use (complex rule: see section 5.2.3)
  - Variables can have same name as methods

### Method Validation
- Only void methods supported
- Parameters are local variables (can be final)
- Must end with return statement
- Method calls only within methods, not global scope
- No method overloading

### Control Flow
- if/while blocks only in methods
- Conditions: boolean constants, variables, or numeric values
- AND/OR operators: `&&` and `||`
- Nested blocks supported to unlimited depth

### Exception Handling
- Custom exception classes (XXXException) in same package as thrower
- Handle: illegal arguments, wrong file format, invalid file names
- Catch specific exceptions, not generic ones

### Compilation & Testing
- Compile with: `javac -Xlint:rawtypes -Xlint:empty -Xlint:divzero -Xlint:deprecation`
- No warnings or errors allowed
- Javadoc must accept the program
- Test with provided presubmission test suite

