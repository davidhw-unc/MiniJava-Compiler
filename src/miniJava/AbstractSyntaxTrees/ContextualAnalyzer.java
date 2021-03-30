package miniJava.AbstractSyntaxTrees;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import miniJava.ErrorReporter;

// TODO change return type back to Object
public class ContextualAnalyzer
        implements Visitor<ContextualAnalyzer.IdentificationTable, TypeDenoter> {
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

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    /////////////////////////////////////////////////////////////////////////////// 

    @Override
    public TypeDenoter visitPackage(Package prog, IdentificationTable table) {
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
    public TypeDenoter visitClassDecl(ClassDecl cd, IdentificationTable table) {
        // Record all members in the table
        table.curMembers.clear();
        for (FieldDecl field : cd.fieldDeclList) {
            if (table.curMembers.containsKey(field.name)) {
                throw error("Identification error - duplicate field declaration", field.posn.line);
            }
            table.curMembers.put(field.name, field);
        }
        for (MethodDecl method : cd.methodDeclList) {
            if (table.curMembers.containsKey(method.name)) {
                throw error("Identification error - duplicate method declaration",
                        method.posn.line);
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
    public TypeDenoter visitFieldDecl(FieldDecl fd, IdentificationTable table) {
        // Visit the TypeDenoter
        fd.getType().visit(this, table);

        return null;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, IdentificationTable table) {
        // TODO remove this once the program is verified working
        // Check that the param layer is present (and empty) on the deque (with no extra layers)
        if (table.curLocals.size() != 1) {
            throw new IllegalStateException("There are old entries left in the curLocals deque");
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

        // Record whether this is a static method
        table.isStatic = md.isStatic;

        // TODO make sure there's a return statement in non-void methods (see @97 on Piazza)

        // Visit each statement within the function
        for (Statement s : md.statementList) {
            s.visit(this, table);
        }

        // Clear the parameter layer's contents (should be only layer left)
        table.curLocals.peek().clear();

        return null;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, IdentificationTable table) {
        // Add this declaration to the table - assumes only the param map is in curLocalsAndParams
        if (table.curLocals.peek().containsKey(pd.name)) {
            throw error("Identification error - duplicate parameter name", pd.posn.line);
        }
        table.curLocals.peek().put(pd.name, pd);

        // Visit the parameter's TypeDenoter
        pd.type.visit(this, table);

        return null;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, IdentificationTable table) {
        // Visit the declaration's TypeDenoter
        decl.type.visit(this, table);

        // Add this declaration to the table
        if (table.curLocals.peek().containsKey(decl.name)) {
            throw error("Identification error - local variable conflicts with parameter"
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
    public TypeDenoter visitBaseType(BaseType type, IdentificationTable table) {
        // Do nothing
        return null;
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, IdentificationTable table) {
        // Find corresponding class in the table
        ClassDecl decl = table.classes.get(type.className);

        // If not found, record a nonfatal error and make the type ERROR
        if (decl == null) {
            error("Type error - unknown class name", type.posn.line);
            type.typeKind = TypeKind.ERROR;
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
    public TypeDenoter visitArrayType(ArrayType type, IdentificationTable table) {
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

    // TODO take into account static environment!!!

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, IdentificationTable table) {
        // Create new frame on the curBlockLocals deque
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
    public TypeDenoter visitVarDeclStmt(VarDeclStmt stmt, IdentificationTable table) {
        // Visit the Expression
        stmt.initExp.visit(this, table);

        // Visit the VarDecl *after* the Expression - this will add it to the table
        stmt.varDecl.visit(this, table);

        // Check type equality
        if (!TypeDenoter.eq(stmt.initExp.getType(), stmt.varDecl.getType())) {
            error("Type error - incompatible types in variable declaration", stmt.posn.line);
        }

        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, IdentificationTable table) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, IdentificationTable table) {
        // TODO check expr type against expectedReturn in table

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    // TODO take into accout static environment!!!

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitNullExpr(NullExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    // TODO double check that these are all getting their types assigned

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, IdentificationTable table) {
        // Set the ref's type to a new TypeDenoter
        ref.setType(new ClassType(table.curClass.name, ref.posn));

        // Visit said TypeDenoter
        ref.getType().visit(this, table);

        return null;
    }

    // Note: b/c of the grammar, we know any function ref can't be further qualified
    // and must be in a CallStmt or CallExpr

    @Override
    public TypeDenoter visitIdRef(IdRef ref, IdentificationTable table) {
        String name = ref.id.spelling;

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
            throw error(
                    "Identification error - referenced a non-static member from a static context",
                    ref.posn.line);
        }

        if (decl == null) {
            throw error("Identification error - could not find any declarations matching the name "
                    + name, ref.posn.line);
        }

        // Record the matching declaration in the identifier
        ref.id.setDecl(decl);

        // Visit the identifier
        ref.id.visit(this, table);

        // Set this reference's type
        ref.setType(ref.id.getType());

        return null;
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, IdentificationTable table) {
        // Visit the previous reference
        ref.ref.visit(this, table);

        boolean allowNonStatic = true;
        Declaration lastDecl = null;
        ClassDecl lastClass = null;

        if (ref.ref instanceof ThisRef) {
            lastClass = table.curClass;

        } else { // IdRef or QualRef
            // Get lastDecl
            if (ref.ref instanceof IdRef) {
                lastDecl = ((IdRef) ref.ref).id.getDecl();
            } else {
                lastDecl = ((QualRef) ref.ref).id.getDecl();
            }

            if (lastDecl instanceof MethodDecl) {
                // If lastRef is pointing to a method, error
                throw error("Identification error - method reference found when an object"
                        + " reference was expected", ref.posn.line);
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
                if (ref.ref.getType().typeKind != TypeKind.CLASS) {
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
            ref.id.setDecl(table.curMembers.get(ref.id.spelling));
            if (ref.id.getDecl() == null) {
                throw error("Identification error - no member called " + ref.id.spelling
                        + " exists in the current class " + lastClass.name, ref.posn.line);
            }
        } else {
            // Handle case where it's in a different class
            ref.id.setDecl(table.publicMembers.get(lastClass.name).get(ref.id.spelling));
            if (ref.id.getDecl() == null) {
                throw error("Identification error - no public member called " + ref.id.spelling
                        + " in " + lastClass.name, ref.posn.line);
            }

            // Make sure this isn't an illegal non-static access
            if (!allowNonStatic && !((MemberDecl) ref.id.getDecl()).isStatic) {
                throw error("Identification error - attempted to access nonstatic member from"
                        + " a static context", ref.posn.line);
            }
        }

        // Visit this identifier
        ref.id.visit(this, table);

        // Set this reference's type
        ref.setType(ref.id.getType());

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public TypeDenoter visitIdentifier(Identifier id, IdentificationTable table) {
        // Assume decl has been assigned, then assign type
        id.setType(id.getDecl().getType());

        return null;
    }

    @Override
    public TypeDenoter visitOperator(Operator op, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, IdentificationTable table) {
        // Do nothing, type is already defined
        return null;
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, IdentificationTable table) {
        // Do nothing, type is already defined
        return null;
    }

}
