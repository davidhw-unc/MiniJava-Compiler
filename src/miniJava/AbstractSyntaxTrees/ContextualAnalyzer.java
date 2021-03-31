package miniJava.AbstractSyntaxTrees;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class ContextualAnalyzer implements Visitor<ContextualAnalyzer.IdentificationTable, Object> {
    /*------------------------------
     * Static members 
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
        // Params are the first layer in the deque

        Map<String, Map<String, MemberDecl>> publicMembers = new HashMap<>();

        ClassDecl curClass = null;
        TypeDenoter curMethodExpectedRet = null;
        boolean stillNeedReturn = false;
        boolean isStatic = false;

        IdentificationTable() {
            curLocals.add(new HashMap<>());
        }
    }

    private static class AnalysisError extends Error {
        private static final long serialVersionUID = 1L;
    }

    /*------------------------------
     * Non-static members 
     *----------------------------*/

    private ErrorReporter err;

    private ContextualAnalyzer(AST ast, ErrorReporter err) {
        this.err = err;
        try {
            run(ast);
        } catch (AnalysisError e) {
        }
    }

    private void run(AST ast) {
        if (!(ast instanceof Package)) {
            throw new IllegalArgumentException("ast must have a Package as its root");
        }

        ast.visit(this, new IdentificationTable());
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
                return ((ClassType) a).className == null || ((ClassType) a).className == null
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
        printStreamClass.methodDeclList.add(new MethodDecl(
                new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null),
                new ParameterDeclList(), new StatementList(), null));
        ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(),
                null);

        // Add all class declarations to the table
        table.classes.put(systemClass.name, systemClass);
        table.classes.put(printStreamClass.name, printStreamClass);
        table.classes.put(stringClass.name, stringClass);
        for (ClassDecl c : prog.classDeclList) {
            if (table.classes.containsKey(c.name)) {
                throw error("Identification error - duplicate class declaration", c.posn.line);
            }
            table.classes.put(c.name, c);
            HashMap<String, MemberDecl> curPublicMembers = new HashMap<>();
            table.publicMembers.put(c.name, curPublicMembers);
            for (FieldDecl field : c.fieldDeclList) {
                if (!field.isPrivate) {
                    curPublicMembers.put(field.name, field);
                }
            }
            for (MethodDecl method : c.methodDeclList) {
                if (!method.isPrivate) {
                    curPublicMembers.put(method.name, method);
                }
            }
        }

        // Analyze each class
        systemClass.visit(this, table);
        printStreamClass.visit(this, table);
        stringClass.visit(this, table);
        for (ClassDecl c : prog.classDeclList) {
            c.visit(this, table);
        }

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
        // Record all members in the table
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

        // Visit each field & method
        for (FieldDecl field : cd.fieldDeclList) {
            field.visit(this, table);
        }
        for (MethodDecl method : cd.methodDeclList) {
            method.visit(this, table);
        }

        // Note: Classes don't have a type, despite technically implementing Typed

        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, IdentificationTable table) {
        // Visit the TypeDenoter
        fd.getType().visit(this, table);

        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, IdentificationTable table) {
        // TODO remove this once the program is verified working
        // Check that the param layer is present (and empty) on the deque (with no extra layers)
        if (table.curLocals.size() != 1) {
            throw new IllegalStateException(
                    "Exepected exactly one layer in the curLocals deque, found "
                            + table.curLocals.size());
        }
        if (table.curLocals.peek().size() != 0) {
            throw new IllegalStateException(
                    "There are old parameters left in curLocal's parameter layer");
        }

        // Visit parameters
        for (ParameterDecl pd : md.parameterDeclList) {
            pd.visit(this, table);
        }

        // Visit & record expected return type so that return statements can be properly checked
        md.getType().visit(this, table);
        table.curMethodExpectedRet = md.getType();

        // Set the stillNeedReturn flag appropriately
        table.stillNeedReturn = !typeEq(table.curMethodExpectedRet, BaseType.void_dummy);

        // Record whether this is a static method
        table.isStatic = md.isStatic;

        // Visit each statement within the function
        for (Statement s : md.statementList) {
            s.visit(this, table);
        }

        // Make sure a return statement was encountered (if needed)
        if (table.stillNeedReturn) {
            error("Type error - no return statement found in non-void method " + md.name,
                    md.posn.line);
        }

        // Clear the parameter layer's contents (should be only layer left)
        table.curLocals.peek().clear();

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

    @Override
    public Object visitBlockStmt(BlockStmt stmt, IdentificationTable table) {
        // Create new frame on the curBlockLocals deque
        // The new layer starts out containing all entries from the previous layer already
        table.curLocals.push(new HashMap<>(table.curLocals.peek()));

        // Visit each statement within the block
        for (Statement s : stmt.sl) {
            s.visit(this, table);
        }

        // Remove this block's frame from the deque
        table.curLocals.pop();

        return null;
    }

    @Override
    public Object visitVarDeclStmt(VarDeclStmt stmt, IdentificationTable table) {
        // Visit the Expression
        stmt.initExp.visit(this, table);

        // Visit the VarDecl *after* the Expression - this will add it to the table
        stmt.varDecl.visit(this, table);

        // Check type equality
        if (!typeEq(stmt.initExp.getType(), stmt.varDecl.getType())) {
            error("Type error - incompatible types in variable declaration",
                    stmt.varDecl.posn.line);
        }

        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, IdentificationTable table) {
        // Visit the Expression
        stmt.val.visit(this, table);

        // Visit the Reference
        stmt.ref.visit(this, table);

        // Make sure this is a reference that *can* be assigned
        if (stmt.ref instanceof ThisRef) {
            error("Type error - cannot reassign \"this\"", stmt.ref.posn.line);
            return null;
        }
        Declaration refDecl = stmt.ref.getId().getDecl();
        if (refDecl instanceof ClassDecl) {
            error("Type error - cannot reassign a class", stmt.ref.posn.line);
            return null;
        }
        if (refDecl instanceof MethodDecl) {
            error("Type error - cannot reassign a method", stmt.ref.posn.line);
            return null;
        }

        // Note: Don't need to check static status, it gets checked when visiting the Reference

        // Check that the types agree
        if (!typeEq(stmt.ref.getType(), stmt.val.getType())) {
            error("Type error - incompatible types in variable assignment", stmt.posn.line);
        }

        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, IdentificationTable table) {
        // Visit the Reference
        stmt.ref.visit(this, table);

        // Visit the indexing Expression
        stmt.ixExpr.visit(this, table);

        // Visit the value Expression
        stmt.exp.visit(this, table);

        // Make sure this reference corresponds to an ArrayDecl
        if (stmt.ref instanceof ThisRef) {
            error("Type error - cannot perform array access on \"this\"", stmt.posn.line);
            return null;
        }
        Declaration refDecl = stmt.ref.getId().getDecl();
        if (refDecl instanceof ClassDecl) {
            error("Type error - cannot perform array access on a class", stmt.ref.posn.line);
            return null;
        }
        if (refDecl instanceof MethodDecl) {
            error("Type error - cannot perform array access on a method", stmt.ref.posn.line);
            return null;
        }
        if (!(stmt.ref.getType() instanceof ArrayType)) {
            error("Type error - cannot perform array access on a non-array type",
                    stmt.ref.posn.line);
            return null;
        }

        // Verify that the indexing Expression has type int
        if (!typeEq(stmt.ixExpr.getType(), BaseType.int_dummy)) {
            error("Type error - cannot index an array with a non-int value", stmt.ixExpr.posn.line);
        }

        // Make sure the array's element type and the value's type agree
        // TODO double check this
        if (!typeEq(((ArrayType) stmt.ref.getType()).eltType, stmt.exp.getType())) {
            error("Type error - incompatible types in array element assignment", stmt.posn.line);
        }

        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, IdentificationTable table) {
        processCall(stmt.methodRef, stmt.argList, stmt.posn, table);

        // Note: No need to check the return type against anything,
        // as this is a function being called without its return being used

        return null;
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

        // Mark that a return statement has been encountered
        table.stillNeedReturn = false;

        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, IdentificationTable table) {
        // Visit conditional expression
        stmt.cond.visit(this, table);

        // Make sure the conditional expression's type is boolean
        if (!typeEq(stmt.cond.getType(), BaseType.bool_dummy)) {
            error("Type error - conditional expression in if statement must have boolean type",
                    stmt.cond.posn.line);
        }

        // Visit thenStatement
        stmt.thenStmt.visit(this, table);

        // Visit elseStatement if present
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, table);
        }

        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, IdentificationTable table) {
        // Visit conditional expression
        stmt.cond.visit(this, table);

        // Make sure the conditional expression's type is boolean
        if (!typeEq(stmt.cond.getType(), BaseType.bool_dummy)) {
            error("Type error - conditional expression in while statment must have boolean type",
                    stmt.cond.posn.line);
        }

        // Visit body
        stmt.body.visit(this, table);

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    // TODO double check that these are all getting their types assigned

    private static final TypeDenoter generic_error_type = new BaseType(TypeKind.ERROR, null);

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, IdentificationTable table) {
        // Visit the Operator & store the validation function that's returned
        @SuppressWarnings("unchecked")
        UnaryOperator<TypeDenoter> func = (UnaryOperator<TypeDenoter>) expr.operator.visit(this,
                table);

        // Visit the operand expression
        expr.operand.visit(this, table);

        // Validate the operand type and set the UnaryExpression's type appropriately
        TypeDenoter exprType = func.apply(expr.operand.getType());
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
        expr.left.visit(this, table);
        expr.right.visit(this, table);

        // Validate the operand types and set the UnaryExpression's type appropriately
        TypeDenoter exprType = func.apply(expr.left.getType(), expr.right.getType());
        if (exprType == null) {
            error("Invalid operand type for unary operator " + expr.operator.spelling,
                    expr.operator.posn.line);
            exprType = generic_error_type;
        }
        expr.setType(exprType);

        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, IdentificationTable table) {
        // Visit the Reference
        expr.ref.visit(this, table);

        // Note: I don't think I need to make sure this reference isn't a class or method-
        // whatever is enclosing this expression should catch that

        // Set the Expression's type to the type of the reference
        expr.setType(expr.ref.getType());

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
            return null;
        }
        Declaration refDecl = expr.ref.getId().getDecl();
        if (refDecl instanceof ClassDecl) {
            error("Type error - cannot perform array access on a class", expr.ref.posn.line);
            return null;
        }
        if (refDecl instanceof MethodDecl) {
            error("Type error - cannot perform array access on a method", expr.ref.posn.line);
            return null;
        }
        if (!(expr.ref.getType() instanceof ArrayType)) {
            error("Type error - cannot perform array access on a non-array type",
                    expr.ref.posn.line);
            return null;
        }

        // Make sure the indexing expression is of type int
        if (!typeEq(expr.ixExpr.getType(), BaseType.int_dummy)) {
            error("Type error - cannot index an array with a non-int value", expr.ixExpr.posn.line);
        }

        // Set type
        expr.setType(((ArrayType) expr.ref.getType()).eltType);

        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, IdentificationTable table) {
        // Process the function call and set expr's type to the return type of the function
        expr.setType(processCall(expr.methodRef, expr.argList, expr.posn, table));

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
        // - Equality checks against class types
        // - Assignment statements to class types
        // This is handled by typeEq - the NullExpr's TypeDenoter will be seen as equal to any
        //     other class types

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

        return null;
    }

    // Note: b/c of the grammar, we know any function ref can't be further qualified
    // and must be in a CallStmt or CallExpr

    @Override
    public Object visitIdRef(IdRef ref, IdentificationTable table) {
        String name = ref.getId().spelling;

        // First check layer 3+
        Declaration decl = table.curLocals.peek().get(name);

        // If not found in layer 3+, check layer 2
        if (decl == null) {
            decl = table.curMembers.get(name);

            // If not found in layer 2, check layer 1
            if (decl == null) {
                decl = table.classes.get(name);
            }
        }

        // If the current context is static, make sure the indicted decl is as well
        if (table.isStatic && (decl != null) && (decl instanceof MemberDecl)
                && (!((MemberDecl) decl).isStatic)) {
            throw error("Identification error - cannot reference a non-static member from a"
                    + " static context", ref.getId().posn.line);
        }

        if (decl == null) {
            throw error("Identification error - no declarations matching the name "
                    + name, ref.getId().posn.line);
        }

        // Record the matching declaration in the identifier
        ref.getId().setDecl(decl);

        // Visit the identifier
        ref.getId().visit(this, table);

        // Set this reference's type
        ref.setType(ref.getId().getType());

        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, IdentificationTable table) {
        // Visit the previous reference
        ref.prevRef.visit(this, table);

        boolean allowNonStatic = true;
        Declaration lastDecl = null;
        ClassDecl lastClass = null;

        if (ref.prevRef instanceof ThisRef) { // Handle ThisRef
            lastClass = table.curClass;

        } else { // Handle IdRef or QualRef
            // Get lastDecl
            lastDecl = ref.prevRef.getId().getDecl();

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
                // Throw error if not referring to an object
                if (!(ref.prevRef.getType() instanceof ClassType)) {
                    throw error("Identification error - attempted to access member of a"
                            + " non-object reference", ref.posn.line);
                }

                // Set lastClass
                lastClass = ((ClassType) lastDecl.getType()).getDecl();
            }
        }

        // Find declaration for this identifier
        if (lastClass == table.curClass) {
            // Handle case where it's in the current class
            ref.getId().setDecl(table.curMembers.get(ref.getId().spelling));
            if (ref.getId().getDecl() == null) {
                throw error(
                        "Identification error - no member called " + ref.getId().spelling
                                + " exists in the current class " + lastClass.name,
                        ref.getId().posn.line);
            }
        } else {
            // Handle case where it's in a different class
            ref.getId().setDecl(table.publicMembers.get(lastClass.name).get(ref.getId().spelling));
            if (ref.getId().getDecl() == null) {
                throw error("Identification error - no public member called " + ref.getId().spelling
                        + " in " + lastClass.name, ref.getId().posn.line);
            }

            // Make sure this isn't an illegal non-static access
            if (!allowNonStatic && !((MemberDecl) ref.getId().getDecl()).isStatic) {
                throw error("Identification error - attempted to access nonstatic member from"
                        + " a static context", ref.getId().posn.line);
            }
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

    // Note: Actually using the return value here!!!
    @SuppressWarnings("incomplete-switch")
    @Override
    public Object visitOperator(Operator op, IdentificationTable table) {
        // Return an appropriate type-checking function based on the operator in question,
        // which returns the type of the operator's expression if the operands are of valid types,
        // and returns null if not (in which case, an error should be reported and the
        // expression's type should be set to ERROR)

        // Binary operators will return a BiPredicate<TypeDenoter, TypeDenoter>
        // Unary operators will return a Predicate<TypeDenoter>
        switch (op.kind) {
            // Binary operators
            case OR:
            case AND:
                // Both operands must be of type boolean
                return (BinaryOperator<TypeDenoter>) (a,
                        b) -> (typeEq(a, BaseType.bool_dummy) && typeEq(b, BaseType.bool_dummy))
                                ? ((a.typeKind == TypeKind.ERROR) ? a : b)
                                : null;

            case LESS_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
            case GREATER_EQUAL:
            case PLUS:
            case MULTIPLY:
            case DIVIDE:
                // Both operands must be of type int
                return (BinaryOperator<TypeDenoter>) (a,
                        b) -> (typeEq(a, BaseType.int_dummy) && typeEq(b, BaseType.int_dummy))
                                ? ((a.typeKind == TypeKind.ERROR) ? a : b)
                                : null;

            case EQUAL_TO:
            case NOT_EQUAL:
                // The two operands just need to have the same type
                return (BinaryOperator<TypeDenoter>) (a,
                        b) -> (typeEq(a, b)) ? ((a.typeKind == TypeKind.ERROR) ? a : b) : null;

            // Unary operator
            case NOT:
                // The operand must be of type boolean
                return (UnaryOperator<TypeDenoter>) (a) -> typeEq(a, BaseType.bool_dummy) ? a
                        : null;

            // Can be a unary or binary operator
            case MINUS:
                switch (op.operandCount) {
                    case 2:
                        // Binary case - both operands must be of type int
                        return (BinaryOperator<TypeDenoter>) (a,
                                b) -> (typeEq(a, BaseType.int_dummy)
                                        && typeEq(b, BaseType.int_dummy))
                                                ? ((a.typeKind == TypeKind.ERROR) ? a : b)
                                                : null;
                    case 1:
                        // Unary case - the operand must be of type int
                        return (UnaryOperator<TypeDenoter>) (a) -> typeEq(a, BaseType.int_dummy) ? a
                                : null;
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
