package miniJava.ContextualAnalyzer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class ContextualAnalyzer implements Visitor<ContextualAnalyzer.IdentificationTable, Object> {
    /*----------------------------*
     * Static members             *
     *----------------------------*/

    public static void runAnalysis(AST ast, ErrorReporter err) {
        new ContextualAnalyzer(ast, err);
    }

    static class IdentificationTable {
        // Note: no need to separate fields and functions, miniJava doesn't allow id repeats
        // (see @83 on piazza)
        Map<String, ClassDecl> classes = new HashMap<>(); // Layer 1
        Map<String, MemberDecl> curMembers = new HashMap<>(); // Layer 2
        Deque<Map<String, LocalDecl>> curLocals = new ArrayDeque<>(); // Layer 3+
        // Params are the first layer in this deque

        Map<String, Map<String, MemberDecl>> publicMembers = new HashMap<>();

        ClassDecl curClass = null;
        TypeDenoter curMethodExpectedRet = null;
        boolean isCurMethodStatic = false;
        boolean stillNeedReturn = false;
        VarDecl activeVarDecl = null;
        long lineForVisitingTernary;

        boolean curInInitialPass = false;

        IdentificationTable() {
            curLocals.add(new HashMap<>());
        }
    }

    private static class AnalysisError extends Error {
        private static final long serialVersionUID = 1L;
    }

    /*----------------------------*
     * Non-static members         *
     *----------------------------*/

    private ErrorReporter err;

    private ContextualAnalyzer(AST ast, ErrorReporter err) {
        if (!(ast instanceof Package)) {
            throw new IllegalArgumentException("ast must have a Package as its root");
        }

        this.err = err;

        try {
            ast.visit(this, new IdentificationTable());
        } catch (AnalysisError e) {
        }
    }

    // For fatal errors (identification errors), throw this function's return
    private AnalysisError error(String e, long line) {
        err.reportError();
        System.out.printf("*** line %d: %s%n", line, e);
        return new AnalysisError();
    }

    // NEVER directly compare TypeKinds for equality!
    // Use this function to ensure that ERROR and UNSUPPORTED are always properly handled.
    // Use instanceof to check for ClassTypes and ArrayTypes
    // (Pairs well with the dummy types in BaseType)
    private static boolean typeEq(TypeDenoter a, TypeDenoter b) {
        // Deal with UNSUPPORTED (never equal)
        if (a.typeKind == TypeKind.UNSUPPORTED || b.typeKind == TypeKind.UNSUPPORTED) {
            return false;
        }

        // Deal with ERROR (always equal)
        if (a.typeKind == TypeKind.ERROR || b.typeKind == TypeKind.ERROR) {
            return true;
        }

        // Deal with everything else
        if (a.typeKind != b.typeKind) {
            if ((a.typeKind == TypeKind.ARRAY && b.typeKind == TypeKind.CLASS
                    && ((ClassType) b).className == null)
                    || (b.typeKind == TypeKind.ARRAY && a.typeKind == TypeKind.CLASS
                            && ((ClassType) a).className == null)) {
                // null (which is stored as a ClassType with null className) is also equal to arrays
                return true;
            }
            return false;
        }
        switch (a.typeKind) { // At this point we know a and b's typeKinds are the same
            case INT:
            case BOOLEAN:
            case VOID:
                // These are always BaseTypes
                return true;
            case CLASS:
                // null className indicates that this is the "null" literal
                return ((ClassType) a).className == null || ((ClassType) b).className == null
                        || ((ClassType) a).getDecl() == ((ClassType) b).getDecl();
            case ARRAY:
                return typeEq(((ArrayType) a).eltType, ((ArrayType) b).eltType);
            default:
                throw new IllegalStateException("This should be impossible to reach!!!");
        }
    }

    private TypeDenoter processCall(Reference methodRef, ExprList argList, SourcePosition posn,
            IdentificationTable table) {
        // Visit methodRef
        methodRef.visit(this, table);

        // Visit each parameter Expression
        for (Expression expr : argList) {
            expr.visit(this, table);
        }

        // Make sure that methodRef is, in fact, pointing to a method
        if (methodRef instanceof ThisRef || !(methodRef.getId().getDecl() instanceof MethodDecl)) {
            error("Type error - cannot call a non-function reference as a function", posn.line);
            return generic_error_type;
        }

        // Store the corresponding MethodDecl
        MethodDecl methodDecl = (MethodDecl) methodRef.getId().getDecl();

        // Check the number of parameters
        if (argList.size() != methodDecl.parameterDeclList.size()) {
            error("Type error - incorrect number of parameters in call to function "
                    + methodDecl.name, posn.line);
        } else {

            // Check the type of each parameter
            for (int i = 0; i < argList.size(); ++i) {
                // Visit the parameter declaration's type (to make sure it's been visited)
                methodDecl.parameterDeclList.get(i).getType().visit(this, table);
                // Check that the types agree
                if (!typeEq(argList.get(i).getType(),
                        methodDecl.parameterDeclList.get(i).getType())) {
                    error("Type error - the type of parameter " + i
                            + " in the function call and the method declaration do not agree",
                            argList.get(i).posn.line);
                }
            }
        }

        return methodRef.getType();
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    /////////////////////////////////////////////////////////////////////////////// 

    @Override
    public Object visitPackage(Package prog, IdentificationTable table) {
        ClassDecl systemClass = new ClassDecl("System", new FieldDeclList(), new MethodDeclList(),
                null);
        systemClass.fieldDeclList
                .add(new FieldDecl(false, true, new ClassType("_PrintStream", null), "out", null));
        ClassDecl printStreamClass = new ClassDecl("_PrintStream", new FieldDeclList(),
                new MethodDeclList(), null);
        printStreamClass.methodDeclList.add(
                new MethodDecl(new FieldDecl(false, true, BaseType.void_dummy, "println", null),
                        new ParameterDeclList(), new StatementList(), null));
        printStreamClass.methodDeclList.get(0).parameterDeclList
                .add(new ParameterDecl(BaseType.int_dummy, "n", null));
        ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(),
                null);

        // Note: I've marked println() as a static method, because in our current implementation it
        // doesn't access System.out in any way (which is really just a dummy _PrintStream object
        // anyway), so this way I avoid having to create that object at all

        // Link the println method in the Package (needed for code generation)
        prog.printlnMethod = printStreamClass.methodDeclList.get(0);

        // Add all class declarations to the table
        table.classes.put(systemClass.name, systemClass);
        table.classes.put(printStreamClass.name, printStreamClass);
        table.classes.put(stringClass.name, stringClass);
        for (ClassDecl c : prog.classDeclList) {
            if (table.classes.containsKey(c.name)) {
                throw error("Identification error - duplicate class declaration", c.posn.line);
            }
            table.classes.put(c.name, c);
        }

        // Perform initial pass to get member types assigned (and nothing else!)
        table.curInInitialPass = true;
        systemClass.visit(this, table);
        printStreamClass.visit(this, table);
        stringClass.visit(this, table);
        for (ClassDecl c : prog.classDeclList) {
            c.visit(this, table);
        }
        table.curInInitialPass = false;

        // Perform main pass
        systemClass.visit(this, table);
        printStreamClass.visit(this, table);
        stringClass.visit(this, table);
        for (ClassDecl c : prog.classDeclList) {
            c.visit(this, table);
        }

        // Check to verify that a main method is present with the correct attributes and parameters
        // If it is present, link the package's mainMethod field to it
        // If it isn't present, throw an error
        // Note: if there are multiple main methods in the package, this will stop after the first
        for (Map<String, MemberDecl> map : table.publicMembers.values()) {
            for (MemberDecl uncastDecl : map.values()) {

                if (uncastDecl instanceof MethodDecl) {
                    MethodDecl decl = (MethodDecl) uncastDecl;

                    if (decl.isStatic && decl.parameterDeclList.size() == 1) {

                        TypeDenoter uncastType = decl.parameterDeclList.get(0).getType();
                        if (uncastType instanceof ArrayType) {
                            ArrayType type = (ArrayType) uncastType;

                            if (type.eltType instanceof ClassType
                                    && ((ClassType) type.eltType).className.equals("String")) {
                                // Found valid main method - can now store it & return safely
                                prog.mainMethod = decl;
                                return null;
                            }
                        }
                    }
                }
            }
        }

        // If no valid main method was ever found, throw an error
        error("Error - Entry point \"public static void main(String[] args)\" not found in package",
                prog.posn.line);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    // Note: Declarations do not need to have their type assigned- it's already known

    @Override
    public Object visitClassDecl(ClassDecl cd, IdentificationTable table) {
        if (table.curInInitialPass) { // First pass
            // Add all public members to the table's publicMembers map
            HashMap<String, MemberDecl> curPublicMembers = new HashMap<>();
            table.publicMembers.put(cd.name, curPublicMembers);
            for (FieldDecl field : cd.fieldDeclList) {
                if (!field.isPrivate) {
                    curPublicMembers.put(field.name, field);
                }
            }
            for (MethodDecl method : cd.methodDeclList) {
                if (!method.isPrivate) {
                    curPublicMembers.put(method.name, method);
                }
            }

            // Perform first-pass visitation to all members
            for (FieldDecl field : cd.fieldDeclList) {
                // This is the ONLY time fields will be visited!
                field.visit(this, table);
            }
            for (MethodDecl method : cd.methodDeclList) {
                method.visit(this, table);
            }

        } else { // Primary pass
            // Record all members in the table's curMembers deque
            table.curMembers.clear();
            for (FieldDecl field : cd.fieldDeclList) {
                if (table.curMembers.containsKey(field.name)) {
                    throw error("Identification error - duplicate member name", field.posn.line);
                }
                table.curMembers.put(field.name, field);
            }
            for (MethodDecl method : cd.methodDeclList) {
                if (table.curMembers.containsKey(method.name)) {
                    throw error("Identification error - duplicate member name", method.posn.line);
                }
                table.curMembers.put(method.name, method);
            }

            // Save this ClassDecl's reference so that ClassTypes can be made for ThisRefs
            table.curClass = cd;

            // Visit each method for the primary pass - do NOT need to visit fields again
            for (MethodDecl method : cd.methodDeclList) {
                method.visit(this, table);
            }
        }

        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, IdentificationTable table) {
        // FieldDecls should ONLY be visited during the first pass!
        if (!table.curInInitialPass) {
            throw new IllegalStateException(
                    "FieldDecls should ONLY be visited during the first pass!");
        }

        // Visit the TypeDenoter
        fd.getType().visit(this, table);

        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, IdentificationTable table) {
        if (table.curInInitialPass) { // First pass

            // Visit return type
            md.getType().visit(this, table);

        } else { // Primary pass
            // Visit parameters
            for (ParameterDecl pd : md.parameterDeclList) {
                pd.visit(this, table);
            }

            // Record expected return type so that return statements can be properly checked
            table.curMethodExpectedRet = md.getType();

            // Set the stillNeedReturn flag appropriately
            table.stillNeedReturn = !typeEq(table.curMethodExpectedRet, BaseType.void_dummy);

            // Record whether this is a static method
            table.isCurMethodStatic = md.isStatic;

            // Visit each statement within the function
            boolean isReturn = false;
            for (Statement s : md.statementList) {
                isReturn = (boolean) s.visit(this, table);
            }

            // Make sure a return statement was encountered at the end (if needed)
            if (!typeEq(table.curMethodExpectedRet, BaseType.void_dummy) && !isReturn) {
                error("Type error - no return statement found at the end of the non-void method "
                        + md.name, md.posn.line);
            }

            // Clear the parameter layer's contents (should be only layer left)
            table.curLocals.peek().clear();
        }

        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, IdentificationTable table) {
        // Add this declaration to the table - assumes only the param map is in curLocalsAndParams
        if (table.curLocals.peek().containsKey(pd.name)) {
            throw error("Identification error - duplicate parameter name", pd.posn.line);
        }
        table.curLocals.peek().put(pd.name, pd);

        // Visit the parameter's TypeDenoter
        pd.getType().visit(this, table);

        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, IdentificationTable table) {
        // Visit the declaration's TypeDenoter
        decl.getType().visit(this, table);

        // Add this declaration to the table
        if (table.curLocals.peek().containsKey(decl.name)) {
            throw error("Identification error - local variable name conflicts with parameter"
                    + " or local variable declaration", decl.posn.line);
        }
        table.curLocals.peek().put(decl.name, decl);

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBaseType(BaseType type, IdentificationTable table) {
        // Do nothing
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, IdentificationTable table) {
        // If this has already been visited, return
        if (type.getDecl() != null) {
            return null;
        }

        // Find corresponding class in the table
        ClassDecl decl = table.classes.get(type.className);

        // If not found, record a fatal error
        if (decl == null) {
            throw error("Identification error - unknown class name " + type.className,
                    type.posn.line);
        }

        // If the class is "String," make it UNSUPPORTED (no error generated here)
        if (type.className.equals("String")) {
            type.typeKind = TypeKind.UNSUPPORTED;
        }

        // Record the class's declaration
        type.setDecl(decl);

        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, IdentificationTable table) {
        // Note: no need to verify that eltType's kind is INT or CLASS- nothing
        // else should ever be allowed through the parser

        // Visit the type held by the array
        type.eltType.visit(this, table);

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////

    // Note: for statements, the return is a Boolean where true indicates that the statment *always*
    // ends with a return statement (if every path is taken and loops run a finite number of times)

    @Override
    public Object visitBlockStmt(BlockStmt stmt, IdentificationTable table) {
        // Create new frame on the curBlockLocals deque
        // The new layer starts out containing all entries from the previous layer already
        table.curLocals.push(new HashMap<>(table.curLocals.peek()));

        // Visit each statement within the block
        boolean isReturn = false;
        for (Statement s : stmt.sl) {
            isReturn = (boolean) s.visit(this, table);
        }

        // Remove this block's frame from the deque
        table.curLocals.pop();

        // Return whether this block ended with a return statement
        return isReturn;
    }

    @Override
    public Object visitVarDeclStmt(VarDeclStmt stmt, IdentificationTable table) {
        // Visit the VarDecl *before* the Expression - this will add it to the table
        stmt.varDecl.visit(this, table);

        // Record this VarDecl as the activeVarDecl in the table
        table.activeVarDecl = stmt.varDecl;

        // Visit the Expression
        stmt.initExp.visit(this, table);

        // Clear activeVarDecl
        table.activeVarDecl = null;

        // Check type equality
        if (!typeEq(stmt.initExp.getType(), stmt.varDecl.getType())) {
            error("Type error - incompatible types in variable declaration",
                    stmt.varDecl.posn.line);
        }

        // Return indication that this isn't a return statement
        return false;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, IdentificationTable table) {
        // Visit the Expression
        stmt.valExpr.visit(this, table);

        // Visit the Reference
        stmt.ref.visit(this, table);

        // Make sure this is a reference that *can* be assigned
        if (stmt.ref instanceof ThisRef) {
            error("Type error - cannot reassign \"this\"", stmt.ref.posn.line);
            return false;
        }
        Declaration refDecl = stmt.ref.getId().getDecl();
        if (refDecl instanceof ClassDecl) {
            error("Type error - cannot reassign a class", stmt.ref.posn.line);
            return false;
        }
        if (refDecl instanceof MethodDecl) {
            error("Type error - cannot reassign a method", stmt.ref.posn.line);
            return false;
        }
        if (stmt.ref instanceof QualRef
                && ((QualRef) stmt.ref).getId().getDecl() == arrayLengthField) {
            error("Cannot reassign an array's .length member", stmt.ref.posn.line);
        }

        // Note: Don't need to check static status, it gets checked when visiting the Reference

        // Check that the types agree
        if (!typeEq(stmt.ref.getType(), stmt.valExpr.getType())) {
            error("Type error - incompatible types in variable assignment", stmt.posn.line);
        }

        // Return indication that this isn't a return statement
        return false;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, IdentificationTable table) {
        // Visit the Reference
        stmt.ref.visit(this, table);

        // Visit the indexing Expression
        stmt.ixExpr.visit(this, table);

        // Visit the value Expression
        stmt.valExp.visit(this, table);

        // Make sure this reference corresponds to an ArrayDecl
        if (stmt.ref instanceof ThisRef) {
            error("Type error - cannot perform array access on \"this\"", stmt.posn.line);
            return false;
        }
        Declaration refDecl = stmt.ref.getId().getDecl();
        if (refDecl instanceof ClassDecl) {
            error("Type error - cannot perform array access on a class", stmt.ref.posn.line);
            return false;
        }
        if (refDecl instanceof MethodDecl) {
            error("Type error - cannot perform array access on a method", stmt.ref.posn.line);
            return false;
        }
        if (!(stmt.ref.getType() instanceof ArrayType)) {
            error("Type error - cannot perform array access on a non-array type",
                    stmt.ref.posn.line);
            return false;
        }

        // Verify that the indexing Expression has type int
        if (!typeEq(stmt.ixExpr.getType(), BaseType.int_dummy)) {
            error("Type error - cannot index an array with a non-int value", stmt.ixExpr.posn.line);
        }

        // Make sure the array's element type and the value's type agree
        if (!typeEq(((ArrayType) stmt.ref.getType()).eltType, stmt.valExp.getType())) {
            error("Type error - incompatible types in array element assignment", stmt.posn.line);
        }

        // Return indication that this isn't a return statement
        return false;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, IdentificationTable table) {
        processCall(stmt.getMethodRef(), stmt.getArgList(), stmt.posn, table);
        // Note: No need to check the return type against anything,
        // as this is a function being called without its return being used

        // Return indication that this isn't a return statement
        return false;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, IdentificationTable table) {
        // Note: Do not exit early from this function!

        // Do different things based on whether this ReturnStmt has an expression
        if (stmt.returnExpr != null) { // If there *is* an Expression:

            // Visit returnExpr
            stmt.returnExpr.visit(this, table);

            // Verify that the current function isn't void
            if (typeEq(table.curMethodExpectedRet, BaseType.void_dummy)) {
                error("Type error - cannot return a value from a void function", stmt.posn.line);
            } else {

                // Verify that the Expression's type matches the expected return type
                if (!typeEq(stmt.returnExpr.getType(), table.curMethodExpectedRet)) {
                    error("Type error - mismatched return type", stmt.posn.line);
                }
            }
        } else { // If there *is not* an Expression:

            // Verify that the current function is void
            if (!typeEq(table.curMethodExpectedRet, BaseType.void_dummy)) {
                error("Type error - empty return statment in a non-void function", stmt.posn.line);
            }
        }

        // Indicate that this *is* a return statement
        return true;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, IdentificationTable table) {
        // Visit conditional expression
        stmt.condExpr.visit(this, table);

        // Make sure the conditional expression's type is boolean
        if (!typeEq(stmt.condExpr.getType(), BaseType.bool_dummy)) {
            error("Type error - conditional expression in if statement must have boolean type",
                    stmt.condExpr.posn.line);
        }

        // Visit thenStatement
        boolean thenEndsInRet = (boolean) stmt.thenStmt.visit(this, table);

        // Report an error if the thenStatement is just a variable declaration
        if (stmt.thenStmt instanceof VarDeclStmt) {
            error("A variable declaration cannot be the solitary statement in a branch of a"
                    + " conditional statement", stmt.thenStmt.posn.line);
        }

        // Visit elseStatement if present
        if (stmt.elseStmt != null) {
            boolean elseEndsInRet = (boolean) stmt.elseStmt.visit(this, table);

            // Report an error if the elseStatement is just a variable declaration
            if (stmt.elseStmt instanceof VarDeclStmt) {
                error("A variable declaration cannot be the solitary statement in a branch of a"
                        + " conditional statement", stmt.thenStmt.posn.line);
            }

            // Use return to indicate whether this if statement *always* ends in a return
            // (this assumes both branches are possible)
            return thenEndsInRet && elseEndsInRet;
        }

        // Without an else statement, the if statement cannot always end in a return
        return false;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, IdentificationTable table) {
        // Visit conditional expression
        stmt.condExpr.visit(this, table);

        // Make sure the conditional expression's type is boolean
        if (!typeEq(stmt.condExpr.getType(), BaseType.bool_dummy)) {
            error("Type error - conditional expression in while statment must have boolean type",
                    stmt.condExpr.posn.line);
        }

        // Visit body
        boolean bodyEndsInRet = (boolean) stmt.body.visit(this, table);

        // Report an error if the body is just a variable declaration
        if (stmt.body instanceof VarDeclStmt) {
            error("A variable declaration cannot be the solitary statement in a branch of a"
                    + " conditional statement", stmt.body.posn.line);
        }

        // Note: Java *allows* a method which will never return to not have any return statement,
        // even if the method indicates a non-void return type.
        // However, it's not possible to verify at compile time that a method will never return,
        // so I'm going to only return true here if the loop's body *always* ends in a return.
        return bodyEndsInRet;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    private static final TypeDenoter generic_error_type = new BaseType(TypeKind.ERROR, null);

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, IdentificationTable table) {
        // Visit the Operator & store the validation function that's returned
        @SuppressWarnings("unchecked")
        UnaryOperator<TypeDenoter> func = (UnaryOperator<TypeDenoter>) expr.operator.visit(this,
                table);

        // Visit the operand expression
        expr.operandExpr.visit(this, table);

        // Validate the operand type and set the UnaryExpression's type appropriately
        TypeDenoter exprType = func.apply(expr.operandExpr.getType());
        if (exprType == null) {
            error("Type error - invalid operand type for unary operator " + expr.operator.spelling,
                    expr.operator.posn.line);
            exprType = generic_error_type;
        }
        expr.setType(exprType);

        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, IdentificationTable table) {
        // Visit the Operator & store the validation function that's returned
        @SuppressWarnings("unchecked")
        BinaryOperator<TypeDenoter> func = (BinaryOperator<TypeDenoter>) expr.operator.visit(this,
                table);

        // Visit the operand expressions
        expr.leftExpr.visit(this, table);
        expr.rightExpr.visit(this, table);

        // Validate the operand types and set the UnaryExpression's type appropriately
        TypeDenoter exprType = func.apply(expr.leftExpr.getType(), expr.rightExpr.getType());
        if (exprType == null) {
            error("Type error - invalid operand type for binary operator " + expr.operator.spelling,
                    expr.operator.posn.line);
            exprType = generic_error_type;
        }
        expr.setType(exprType);

        return null;
    }

    @Override
    public Object visitTernaryExpr(TernaryExpr te, IdentificationTable table) {
        // Visit the Operator & store the validation function that's returned
        table.lineForVisitingTernary = te.posn.line;
        @SuppressWarnings("unchecked")
        TernaryOperator<TypeDenoter> func = (TernaryOperator<TypeDenoter>) te.operator.visit(this,
                table);

        // Visit the three expressions
        te.leftExpr.visit(this, table);
        te.midExpr.visit(this, table);
        te.rightExpr.visit(this, table);

        // Validate the types
        TypeDenoter exprType = func.apply(te.leftExpr.getType(), te.midExpr.getType(),
                te.rightExpr.getType());
        if (exprType == null) {
            error("Type error - middle and right expression types in ternary operator do not match",
                    te.posn.line);
            exprType = generic_error_type;
        }
        te.setType(exprType);

        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, IdentificationTable table) {
        // Visit the Reference
        expr.ref.visit(this, table);

        // Note: I don't think I need to make sure this reference isn't a class or method-
        // whatever is enclosing this expression should catch that
        /* Follow-up note: I was wrong - case in point:
        class fail335 {     
            public static void main(String[] args) {
                F02 c = F02;
            }
        }
        class F02 {
            public int x;
        }
        */

        // Make sure this reference isn't a class or method
        if (expr.ref.getId().getDecl() instanceof ClassDecl) {
            error("Type error - cannot refer directly to a class", expr.ref.posn.line);
            expr.setType(generic_error_type);
        } else if (expr.ref.getId().getDecl() instanceof MethodDecl) {
            error("Type error - cannot refer directly to a method identifier", expr.ref.posn.line);
            expr.setType(generic_error_type);
        } else {

            // Set the Expression's type to the type of the reference
            expr.setType(expr.ref.getType());
        }

        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, IdentificationTable table) {
        // Visit the reference
        expr.ref.visit(this, table);

        // Visit the indexing expression
        expr.ixExpr.visit(this, table);

        // Make sure the reference corresponds to an ArrayDecl
        if (expr.ref instanceof ThisRef) {
            error("Type error - cannot perform array access on \"this\"", expr.posn.line);
            expr.setType(generic_error_type);
            return null;
        } else {
            Declaration refDecl = expr.ref.getId().getDecl();
            if (refDecl instanceof ClassDecl) {
                error("Type error - cannot perform array access on a class", expr.ref.posn.line);
                expr.setType(generic_error_type);
                return null;
            }
            if (refDecl instanceof MethodDecl) {
                error("Type error - cannot perform array access on a method", expr.ref.posn.line);
                expr.setType(generic_error_type);
                return null;
            }
            if (!(expr.ref.getType() instanceof ArrayType)) {
                error("Type error - cannot perform array access on a non-array type",
                        expr.ref.posn.line);
                expr.setType(generic_error_type);
                return null;
            }
        }

        // Make sure the indexing expression is of type int
        if (!typeEq(expr.ixExpr.getType(), BaseType.int_dummy)) {
            error("Type error - cannot index an array with a non-int value", expr.ixExpr.posn.line);
            expr.setType(generic_error_type);
            return null;
        }

        // Set type
        expr.setType(((ArrayType) expr.ref.getType()).eltType);

        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, IdentificationTable table) {
        // Process the function call and set expr's type to the return type of the function
        expr.setType(processCall(expr.getMethodRef(), expr.getArgList(), expr.posn, table));

        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, IdentificationTable table) {
        // Visit the Literal
        expr.lit.visit(this, table);

        // Set the Expression's type to match the Literal
        expr.setType(expr.lit.getType());

        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, IdentificationTable table) {
        // Visit the ClassType
        expr.classtype.visit(this, table);

        // Set the Expression's type to be the ClassType
        expr.setType(expr.classtype);

        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, IdentificationTable table) {
        // Note: Parser should ensure that eltType is always either int or a class

        // Visit eltType
        expr.eltType.visit(this, table);

        // Visit sizeExpr
        expr.sizeExpr.visit(this, table);

        // Ensure that sizeExpr has type int
        if (!typeEq(expr.sizeExpr.getType(), BaseType.int_dummy)) {
            error("Type error - attempted to declare a new array with a non-int size expression",
                    expr.sizeExpr.posn.line);
        }

        // Set this Expression's type to a new ArrayType based on EltType
        expr.setType(new ArrayType(expr.eltType, expr.eltType.posn));

        return null;
    }

    @Override
    public Object visitNullExpr(NullExpr expr, IdentificationTable table) {
        // NullExprs need to be valid in two places:
        // - Equality checks against object* types
        // - Assignment statements to object* types
        //
        // * Both classes and arrays are objects
        //
        // This is handled by typeEq - the NullExpr's TypeDenoter will be seen as equal to *any*
        //     other object types

        // Set expr's type to a special ClassType with null className (and no decl)
        expr.setType(new ClassType(null, expr.posn));

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitThisRef(ThisRef ref, IdentificationTable table) {
        // Set the ref's type to a new TypeDenoter
        ref.setType(new ClassType(table.curClass.name, ref.posn));

        // Visit said TypeDenoter
        ref.getType().visit(this, table);

        // Throw an error if this is in a static context
        if (table.isCurMethodStatic) {
            error("Identification error - cannot reference \"this\" from a static context",
                    ref.posn.line);
        }

        return null;
    }

    // Note: b/c of the grammar, we know any function ref can't be further qualified
    // and must be in a CallStmt or CallExpr

    @Override
    public Object visitIdRef(IdRef ref, IdentificationTable table) {
        String name = ref.getId().spelling;

        // First check layer 3+
        Declaration decl = table.curLocals.peek().get(name);

        if (decl != null) {
            // If found in layer 3+, make sure it isn't currently being initialized
            if (decl == table.activeVarDecl) {
                error("Identification error - cannot reference a variable in its own initializer",
                        ref.posn.line);
            }

        } else {
            // If not found in layer 3+, check layer 2
            decl = table.curMembers.get(name);

            // If not found in layer 2, check layer 1
            if (decl == null) {
                decl = table.classes.get(name);
            }
        }

        // If the current context is static, make sure the indicted decl is as well
        if (table.isCurMethodStatic && (decl != null) && (decl instanceof MemberDecl)
                && (!((MemberDecl) decl).isStatic)) {
            throw error("Identification error - cannot reference a non-static member from a"
                    + " static context", ref.getId().posn.line);
        }

        // Throw error if the name can't be found
        if (decl == null) {
            throw error("Identification error - no declarations matching the name " + name,
                    ref.getId().posn.line);
        }

        // Record the matching declaration in the identifier
        ref.getId().setDecl(decl);

        // Visit the identifier
        ref.getId().visit(this, table);

        // Set this reference's type
        ref.setType(ref.getId().getType());

        return null;
    }

    public static final FieldDecl arrayLengthField = new FieldDecl(false, false, BaseType.int_dummy,
            "length", null);

    @SuppressWarnings("null")
    @Override
    public Object visitQualRef(QualRef ref, IdentificationTable table) {
        // Visit the previous reference
        ref.prevRef.visit(this, table);

        boolean allowNonStatic = true;
        ClassDecl lastClass = null;

        if (ref.prevRef instanceof ThisRef) { // Handle ThisRef
            lastClass = table.curClass;

        } else { // Handle IdRef, ArrayRef, or QualRef
            // Get lastDecl
            Declaration lastDecl = ref.prevRef.getId().getDecl();

            if (lastDecl instanceof MethodDecl) {
                // If lastRef is pointing to a method, error
                throw error("Identification error - cannot access member of a method",
                        ref.posn.line);
            }

            // Check if the last matched decl was a class
            // (Can only happen if ref.ref was an IdRef)
            if (lastDecl instanceof ClassDecl) {
                // If ref.ref is pointing to a class, only allow static access
                allowNonStatic = false;

                // Set lastClass
                lastClass = (ClassDecl) lastDecl;

            } else { // Pointing to FieldDecl, VarDecl, or ParameterDecl

                // Throw error if not referring to an object (arrays are allowed)
                if (!(ref.prevRef.getType() instanceof ClassType
                        || ref.prevRef.getType() instanceof ArrayType)) {
                    throw error("Identification error - attempted to access member of a"
                            + " non-object reference", ref.posn.line);
                }

                if (ref.prevRef.getType() instanceof ClassType) {
                    // Set lastClass
                    lastClass = ((ClassType) lastDecl.getType()).getDecl();
                }
            }
        }

        // Note: Eclipse warns that lastClass can be null here.
        //  This will only happen when the previous ref in the chain was pointing to an array,
        //      which is handled in this first if block.
        //  This means that lastClass shouldn't be accessed while it's null, so I felt safe adding
        //      the @SuppressWarnings("null") annotation to this method.
        if (ref.prevRef.getType() instanceof ArrayType) {
            // Handle arrays (have .length field)
            if (!ref.getId().spelling.equals("length")) {
                // Throw error if the member's name is anything other than "length"
                throw error("Identification error - no member called " + ref.getId().spelling
                        + " in array objects", ref.getId().posn.line);
            }

            ref.getId().setDecl(arrayLengthField);

        } else if (lastClass == table.curClass) {
            // Find declaration for this identifier
            // Handle case where it's in the current class
            ref.getId().setDecl(table.curMembers.get(ref.getId().spelling));
            if (ref.getId().getDecl() == null) {
                throw error(
                        "Identification error - no member called " + ref.getId().spelling
                                + " exists in the current class " + lastClass.name,
                        ref.getId().posn.line);
            }
        } else {
            // Handle case where it's in a different class (only allow access to public members)
            ref.getId().setDecl(table.publicMembers.get(lastClass.name).get(ref.getId().spelling));
            if (ref.getId().getDecl() == null) {
                throw error("Identification error - no public member called " + ref.getId().spelling
                        + " in " + lastClass.name, ref.getId().posn.line);
            }
        }

        // Make sure this isn't an illegal non-static access
        if (!allowNonStatic && !((MemberDecl) ref.getId().getDecl()).isStatic) {
            throw error("Identification error - attempted to access nonstatic member from"
                    + " a static context", ref.getId().posn.line);
        }

        // Visit this identifier
        ref.getId().visit(this, table);

        // Set this reference's type
        ref.setType(ref.getId().getType());

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier id, IdentificationTable table) {
        // Assumes decl has been assigned already

        // Assign this Identifier's type
        id.setType(id.getDecl().getType());

        return null;
    }

    @FunctionalInterface
    private interface TernaryOperator<T> {
        public T apply(T a, T b, T c);
    }

    // Note: Actually using the return value here!!!
    @SuppressWarnings("incomplete-switch")
    @Override
    public Object visitOperator(Operator op, IdentificationTable table) {
        // Return an appropriate type-checking function based on the operator in question,
        // which returns the type of the operator's expression if the operands are of valid types,
        // and returns null if not (in which case, an error should be reported and the
        // expression's type should be set to ERROR)

        // Ternary operators will return a TernaryOperator<TypeDenoter>
        // Binary operators will return a BinaryOperator<TypeDenoter>
        // Unary operators will return a UnaryOperator<TypeDenoter>
        switch (op.kind) {
            // Ternary operators
            case Q_MARK:
                return (TernaryOperator<TypeDenoter>) (a, b, c) -> {
                    if (!typeEq(a, BaseType.bool_dummy)) {
                        error("Type error - ternary expression has non-boolean conditional",
                                table.lineForVisitingTernary);
                    }
                    return typeEq(b, c)
                            ? b.typeKind == TypeKind.ERROR || c.typeKind == TypeKind.ERROR
                                    ? generic_error_type
                                    : b
                            : null;
                };

            // Binary operators
            case OR:
            case AND:
                // Both operands must be of type boolean
                // Produces a boolean
                return (BinaryOperator<TypeDenoter>) (a,
                        b) -> (typeEq(a, BaseType.bool_dummy) && typeEq(b, BaseType.bool_dummy))
                                ? BaseType.bool_dummy
                                : null;

            case LESS_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
            case GREATER_EQUAL:
                // Both operands must be of type int
                // Produces a boolean
                return (BinaryOperator<TypeDenoter>) (a,
                        b) -> (typeEq(a, BaseType.int_dummy) && typeEq(b, BaseType.int_dummy))
                                ? BaseType.bool_dummy
                                : null;
            case PLUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULUS:
                // Both operands must be of type int
                // Produces an int
                return (BinaryOperator<TypeDenoter>) (a,
                        b) -> (typeEq(a, BaseType.int_dummy) && typeEq(b, BaseType.int_dummy))
                                ? BaseType.int_dummy
                                : null;

            case EQUAL_TO:
            case NOT_EQUAL:
                // The two operands just need to have the same type
                // Produces a boolean
                return (BinaryOperator<TypeDenoter>) (a, b) -> (typeEq(a, b)) ? BaseType.bool_dummy
                        : null;

            // Unary operator
            case NOT:
                // The operand must be of type boolean
                // Produces a boolean
                return (UnaryOperator<TypeDenoter>) (
                        a) -> typeEq(a, BaseType.bool_dummy) ? BaseType.bool_dummy : null;

            // Can be a unary or binary operator
            case MINUS:
                switch (op.operandCount) {
                    case 2:
                        // Binary case - both operands must be of type int
                        // Produces an int
                        return (BinaryOperator<TypeDenoter>) (a,
                                b) -> (typeEq(a, BaseType.int_dummy)
                                        && typeEq(b, BaseType.int_dummy)) ? BaseType.int_dummy
                                                : null;
                    case 1:
                        // Unary case - the operand must be of type int
                        // Produces an int
                        return (UnaryOperator<TypeDenoter>) (
                                a) -> typeEq(a, BaseType.int_dummy) ? BaseType.int_dummy : null;
                }
        }

        throw new IllegalStateException("Should not be possible to reach this!");
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, IdentificationTable table) {
        // Do nothing, type is already defined
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, IdentificationTable table) {
        // Do nothing, type is already defined
        return null;
    }

}
