# Plan: s-Java Code Verifier Implementation

Build a Java verifier tool that validates simplified Java syntax (s-Java) using regular expressions and exception handling. The program reads an s-Java source file, preprocesses it, builds a scope tree, validates the code structure, and outputs 0 (valid), 1 (syntax error), or 2 (IO error) with descriptive error messages.

## Architecture Overview

The system follows a **single-pass validation architecture** where validation happens during parsing:
1. **File Reading** → 2. **Parser (entry point)** → 3. **Preprocessor** → 4. **Scope Tree Building with Simultaneous Validation** → 5. **Output**

**Key Insight**: Validation is integrated into the scope validation process, not separated. Each scope validates its content as it's parsed, and nested scopes are validated before parent scopes continue.

### Package Structure
- `ex5.main` - Entry point (`Sjavac` class) + `InvalidFileException` (thrown by main)
  - Main only calls `CodeParser.parse(filePath)`
  - Parser handles everything else internally
- `ex5.preprocessor` - Line cleanup (`CodePreprocessor`) + `PreprocessorException`
- `ex5.parser` - Parsing logic (`CodeParser`) + `ParserException`
- `ex5.models` - Data models (`ProcessedLine`, `Variable`, `VariableTable`, `Method`, `Statement`)
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
- **`ProcessedLine` class**:
  - Represents a single line from the preprocessed file
  - Attributes: `int originalLineNumber`, `String content`
  - Used by preprocessor, parser, and all scopes
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
  - **Removing supported comment lines**: lines where first non-whitespace chars are `//`
  - **Removing empty lines**: lines containing only whitespace
  - Trimming leading/trailing whitespace from code lines
  - Preserving original line numbers for error reporting (track line number mapping)
  - **NOT checking for illegal comments** (`/* */`, `/** */`, mid-line `//`) - these will be caught during validation
- Output: `List<ProcessedLine>` (from `ex5.models` package)
- **Note**: Do NOT normalize internal whitespace - validation must check for required spaces:
  - Between `void` and method name (e.g., `voidfoo()` is illegal)
  - Between `final` and type (e.g., `finalint` is illegal)
  - Between type and variable name (e.g., `inta` is illegal)
- Throw `PreprocessorException` only for IO errors, not for syntax issues

### 4. Implement RegexManager (`ex5.validator.RegexManager`)
- Create `RegexManager` class with static final `Pattern` constants and type compatibility map:
  - **Type Compatibility Map**:
    - `static final Map<String, Set<String>> TYPE_COMPATIBILITY` - Maps types to types that can be assigned to them
    - Example:
      ```java
      TYPE_COMPATIBILITY.put("int", Set.of("int"));
      TYPE_COMPATIBILITY.put("double", Set.of("int", "double"));
      TYPE_COMPATIBILITY.put("boolean", Set.of("int", "double", "boolean"));
      TYPE_COMPATIBILITY.put("String", Set.of("String"));
      TYPE_COMPATIBILITY.put("char", Set.of("char"));
      ```
    - Use: `RegexManager.isTypeCompatible(String targetType, String sourceType)` returns `TYPE_COMPATIBILITY.get(targetType).contains(sourceType)`
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
    - `static Map<String, Method> globalMethods` - **Static registry of all methods in the file** (shared across all scopes)
  - Methods:
    - `addVariable(Variable v)` - Add to local variable table
    - `findVariable(String name)` - Search this scope, then recursively search parent
    - `addStatement(Statement s)` - Add statement to list
    - `addChildScope(Scope child)` - Link child scope
    - `abstract void validate()` - Validate this scope (implemented by subclasses)
    - `getDepth()` - Calculate nesting depth
    - `static void registerMethod(Method m)` - Add method to global registry (call during GlobalScope parsing)
    - `static Method getMethod(String name)` - Retrieve method from global registry
    - `static boolean methodExists(String name)` - Check if method is defined
    - **`protected void validateStatements(List<ProcessedLine> lines, int startIdx, int endIdx)`** - **Common method** for parsing and validating statements line-by-line (shared by MethodScope and BlockScope):
      - Variable declarations → parse, add to scope, validate
      - Assignments → validate variable exists, types match, initialized
      - Method calls → validate method exists, args match
      - if/while statements → create BlockScope, immediately call its validate()
      - return statements → mark and validate (MethodScope only)
      - Closing `}` → return control to parent
- **`GlobalScope extends Scope`**:
  - Additional attributes:
    - `List<ProcessedLine> lines` - Preprocessed lines received from parser
  - Additional methods:
    - `validate(List<ProcessedLine> lines)` - **Global Scope Validation Process**:
      1. Store preprocessed lines
      2. Parse global scope:
         - Iterate through all preprocessed lines
         - Identify and list all global variable declarations
         - Identify and list all method signatures (register them via `Scope.registerMethod()`)
         - Track `{` and `}` to know which lines belong to each method
      3. **Phase 1**: Register all methods in static registry
      4. **Phase 2**: Validate global variables before methods
         - Validate each global variable: syntax, naming, type, initialization
         - Check for duplicate names
      5. **Phase 3**: Call `validate()` on each method's scope
         - For each `MethodScope` in order, call its `validate()` method
         - Pass relevant line range to each MethodScope
      6. Return success if all validation passes
- **`MethodScope extends Scope`**:
  - Additional attributes:
    - `Method methodDefinition` - Reference to Method object
    - `int lastStatementLine` - Track line number of last non-empty statement (to check if it's return)
  - Additional methods:
    - `addParameter(Variable param)` - Add parameter to variable table
    - `recordStatement(int lineNumber, StatementType type)` - Track statements, update lastStatementLine
    - `validate(List<ProcessedLine> lines, int startIdx, int endIdx)` - **Method Scope Validation Process**:
      1. **Parse and validate method signature**: name, parameters (syntax, types, naming, final handling)
      2. **Add parameters to variable table** (mark as initialized)
      3. **Parse method body** using **`validateStatements(lines, bodyStartIdx, endIdx)`** (inherited from Scope):
         - This method is shared with BlockScope
         - Handles: variable declarations, assignments, method calls, if/while blocks, return
      4. **Validate return statement**: Check if statement at `lastStatementLine` is a return statement
         - Don't care about return statements in the middle of the method
      5. Return success if all validation passes
      
  **Note**: The only difference from BlockScope is:
  - MethodScope validates parameters at start
  - MethodScope validates that last statement is return at end
  - Inner statement validation is identical (uses shared `validateStatements()` method)
- **`BlockScope extends Scope`** (for if/while):
  - Additional attributes:
    - `String blockType` - "if" or "while"
    - `String condition` - Condition expression
    - `int blockEndLine` - Line where closing `}` is
  - Additional methods:
    - `validateCondition()` - Validate condition syntax and variable references
    - `validate(List<ProcessedLine> lines, int startIdx, int endIdx)` - **Block Scope Validation Process**:
      1. **Parse and validate condition**: syntax, variables exist and initialized
      2. **Parse block contents** using **`validateStatements(lines, bodyStartIdx, endIdx)`** (inherited from Scope):
         - This method is shared with MethodScope
         - Handles: variable declarations, assignments, method calls, nested if/while blocks
      3. Return control to parent with `blockEndLine` set
      
  **Note**: The only difference from MethodScope is:
  - BlockScope validates condition at start (instead of parameters)
  - BlockScope does NOT validate return statement
  - Inner statement validation is identical (uses shared `validateStatements()` method)

### 6. Implement Parser (`ex5.parser.CodeParser`)
- Create `CodeParser` class as the main entry point for validation:
  - **`GlobalScope parse(String filePath)` method** (throws exceptions):
    1. **Call preprocessor**: `List<ProcessedLine> lines = CodePreprocessor.preprocess(filePath)`
    2. Create `GlobalScope` instance
    3. Call `globalScope.validate(lines)` - pass preprocessed lines to GlobalScope
    4. Return the validated `GlobalScope`
- **Parser responsibilities**:
  - Coordinate preprocessor and scope validation
  - Pass preprocessed lines to GlobalScope
  - Handle exceptions from preprocessor and validation
- **GlobalScope responsibilities**:
  - Receive preprocessed lines from parser
  - Parse and validate using those lines
  - Pass line ranges to MethodScope and BlockScope as needed
- Use `RegexManager` patterns throughout for line type identification
- Throw `ParserException` with line number for parsing errors
- Line number tracking: Preprocessor maintains mapping from preprocessed line index to original file line number

### 7. Implement Validators (integrated into Scope.validate() methods)
Validation logic is implemented in helper classes and called from within each scope's validate() method:

- **`VariableValidator`** (static utility class):
  - `validateVariableName(String name)` - Check naming rules (no leading digit, no `__`, etc.)
  - `validateTypeCompatibility(String targetType, String sourceType)` - Check if assignment is valid using `RegexManager.isTypeCompatible(targetType, sourceType)`
  - `validateVariableUsage(Scope, String varName, int lineNumber)` - Check variable initialized before use
    - **Critical Rule from 5.2.1**: Local variable not initialized at declaration can only be used if:
      1. Next line where it appears is an assignment to it, OR
      2. It is never used
    - **Critical Rule from 5.2.3**: When inside a method:
      - Global variables initialized anywhere in the method are considered initialized for rest of method
      - Local variables initialized in nested scope are considered initialized in parent scope for subsequent lines
      - You do NOT need to check if initialization is reachable (e.g., inside `if(false)` is OK)
  - `validateFinalVariable(Variable)` - Check final variables initialized at declaration, not reassigned
  - `validateScopeAccess(Scope, String varName)` - Verify variable accessible from current scope

- **`MethodValidator`** (static utility class):
  - `validateMethodSignature(String methodName, List<Variable> params)` - Check naming (must start with letter), parameters
    - **Note**: Only void methods are supported, no return type validation needed
  - `validateMethodCall(String methodName, List<String> args)` - Check method exists via `Scope.methodExists()`, argument count and types match parameters
    - Use `RegexManager.isTypeCompatible()` for parameter type checking
  - `validateLastStatementIsReturn(int lastStatementLine, List<ProcessedLine> lines)` - Verify last statement is return (ignoring empty lines after)

- **`ControlFlowValidator`** (static utility class):
  - `validateCondition(String condition, Scope scope)` - Parse condition, check boolean expression syntax
  - `validateConditionVariables(String condition, Scope scope)` - Verify variables in condition exist and are initialized
  - `validateOperators(String condition)` - Check `&&` and `||` placement (between expressions, not at start/end)

- **RegexManager usage**:
  - Called from validators to parse and match patterns
  - Patterns are used during both parsing and validation

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
          
          // 2. Parse and validate the file
          // Parser internally calls preprocessor and validates scopes
          CodeParser parser = new CodeParser();
          GlobalScope globalScope = parser.parse(filePath);
          
          // 3. Success - code is valid
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
- Main is very simple: only validates arguments and calls parser
- Parser handles all logic including preprocessor and scope validation

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

## Data Flow and Control Flow

### Execution Flow
```
main(args[])
  └─> CodeParser.parse(filePath)
       ├─> CodePreprocessor.preprocess(filePath) [Parser calls preprocessor - reads file, removes comments/empty lines]
       │    └─> returns List<ProcessedLine>
       └─> GlobalScope.validate(lines) [Parser passes preprocessed lines to GlobalScope]
            ├─> [Global parsing phase 1] List all methods, register in static registry
            ├─> [Global parsing phase 2] Validate all global variables
            └─> [Global parsing phase 3] For each method:
                 └─> MethodScope.validate(lines, startIdx, endIdx) [Receives line range]
                      ├─> Validate parameters
                      └─> validateStatements(lines, bodyStart, endIdx) [SHARED METHOD from base Scope]
                           ├─> Variable declaration? Add to scope, validate
                           ├─> Assignment? Validate variable exists, types match
                           ├─> Method call? Validate method exists (via static registry)
                           ├─> if/while? Create BlockScope and immediately call:
                           │    └─> BlockScope.validate(lines, startIdx, endIdx) [Receives line range]
                           │         ├─> Validate condition
                           │         └─> validateStatements(lines, bodyStart, endIdx) [SHARED METHOD from base Scope]
                           │              ├─> Can have nested blocks → recursive validate()
                           │              └─> When closing } reached, return with blockEndLine
                           │    └─> [Continue from line after }]
                           └─> return? Validate and mark
                      └─> Validate return exists and is last
       └─> return GlobalScope
  └─> System.out.println(0) [success]
  └─> catch SyntaxException → System.err + System.out.println(1)
  └─> catch InvalidFileException → System.err + System.out.println(2)
```

### Key Architectural Differences from Original Plan
1. **Parser calls preprocessor** - CodeParser.parse() calls CodePreprocessor.preprocess() at the start
2. **Scopes receive preprocessed lines** - Parser passes lines to GlobalScope, which passes line ranges to child scopes
3. **Shared validation logic** - MethodScope and BlockScope share `validateStatements()` method for inner statements
4. **MethodScope vs BlockScope** - Only differ in parameter/condition validation and return checking
5. **Nested scope handling** - Immediately validate when encountered, parent continues after
6. **Method lookup via static registry** - Register all methods before validating any method body

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
**Question**: Should preprocessor only remove supported comments and empty lines?
**Recommendation**: Yes. Remove `//` comments and empty lines completely. Do NOT report errors for unsupported comment formats (`/* */`, etc.) - those will be caught during validation when the parser doesn't recognize them.

### 2. Single-Pass Validation vs. Two-Pass
**Question**: Build complete scope tree first, then validate? Or validate during parsing?
**Recommendation**: Validate during scope traversal (single-pass). Key advantages:
- Stop at first error immediately without building unnecessary scope structures
- Simpler memory usage (don't store entire tree)
- Natural recursive structure matches scope hierarchy
- Nested scopes are validated immediately when encountered, parent continues after
- Still allows forward method references via static method registry

### 3. Method Registry Implementation
**Question**: How to handle forward method references without building tree first?
**Recommendation**: Use static `Map<String, Method> globalMethods` in base `Scope` class:
- During global scope parsing, first pass registers all method signatures
- Second pass validates global variables
- Third pass validates each method, which can safely call any registered method

### 4. Line Number Mapping
**Question**: How to track original line numbers when preprocessor removes lines?
**Recommendation**: Maintain mapping from preprocessed line index to original file line number. When reporting errors, use original line number.

### 5. Statement Storage Necessity
**Question**: Do we need to store Statement objects during parsing?
**Recommendation**: Minimal use - only track statement type and line for return validation. Don't build full tree.

### 6. Scope Boundary Detection
**Question**: How to know where methods/blocks end?
**Recommendation**: Track opening `{` and closing `}` braces while parsing. Each `{` increments brace depth, each `}` decrements. Method ends when brace depth returns to 0.

### 7. Regex Pattern Reuse
**Question**: How much validation logic should go in RegexManager vs. validator classes?
**Recommendation**: RegexManager provides pattern matching and group extraction. Validator classes provide semantic validation (e.g., "variable exists", "type compatible").

### 8. Implementation Order: Validators Before Scopes?
**Question**: Should we implement validators (Step 7) before scopes (Step 5)?
**Recommendation**: Current order (Scopes before Validators) makes sense because:
- Validators are **utility classes called by Scopes** - understanding what Scopes need helps design validators
- Scopes define the validation requirements - validators implement the validation logic
- Can argue either way, but current order follows "define requirements → implement validation" pattern
**Alternative**: Could implement RegexManager + basic validators first, then scopes. Both approaches work.

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

