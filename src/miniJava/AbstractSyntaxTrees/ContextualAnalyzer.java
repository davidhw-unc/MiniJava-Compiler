package miniJava.AbstractSyntaxTrees;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, ClassDecl> classes;
        Map<String, FieldDecl> memberFs;
        Map<String, MethodDecl> memberMs;
        Map<String, ParameterDecl> params;
        Deque<Map<String, VarDecl>> locals = new ArrayDeque<>();
        Map<String, Map<String, FieldDecl>> publicDeclFs = new HashMap<>();
        Map<String, Map<String, MethodDecl>> publicDeclMs = new HashMap<>();
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

    private AnalysisError error(String e, long line) {
        err.reportError(String.format("Contextual analysis error on line %d: %s", line, e));
        System.out.printf("*** line %d: %s%n", line, e);
        return new AnalysisError();
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    /////////////////////////////////////////////////////////////////////////////// 

    @Override
    public Object visitPackage(Package prog, IdentificationTable table) {
        ClassDecl systemClass = new ClassDecl("System", new FieldDeclList(), new MethodDeclList(),
                new SourcePosition(0, 0));
        systemClass.fieldDeclList.add(
                new FieldDecl(false, true, new ClassType("_PrintStream", new SourcePosition(0, 0)),
                "out", new SourcePosition(0, 0)));
        ClassDecl printStreamClass = new ClassDecl("_PrintStream", new FieldDeclList(),
                new MethodDeclList(), new SourcePosition(0, 0));
        printStreamClass.methodDeclList.add(new MethodDecl(
                new FieldDecl(false, false, BaseType.getType(TypeKind.VOID), "println",
                        new SourcePosition(0, 0)),
                new ParameterDeclList(), new StatementList(), new SourcePosition(0, 0)));
        ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), new SourcePosition(0, 0)) {
            @Override
            public TypeDenoter getAndCheckType(TypeDenoter... types) {
                return BaseType.getType(TypeKind.UNSUPPORTED);
            }
        };
        //ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(),
        //        new SourcePosition(0, 0));

        // Add all class declarations to highest scope level
        table.classes.put(systemClass.name, systemClass);
        table.classes.put(printStreamClass.name, printStreamClass);
        table.classes.put(stringClass.name, stringClass);
        for (ClassDecl c : prog.classDeclList) {
            if (table.classes.containsKey(c.name)) {
                throw error("Identification error - duplicate class declaration", c.posn.line);
            }
            table.classes.put(c.name, c);
            HashMap<String, MethodDecl> publicDeclMs = new HashMap<>();
            table.publicDeclMs.put(c.name, publicDeclMs);
            HashMap<String, FieldDecl> publicDeclFs = new HashMap<>();
            table.publicDeclFs.put(c.name, publicDeclFs);
            for (FieldDecl field : c.fieldDeclList) {
                if (!field.isPrivate) {
                    publicDeclFs.put(field.name, field);
                }
            }
            for (MethodDecl method : c.methodDeclList) {
                if (!method.isPrivate) {
                    publicDeclMs.put(method.name, method);
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

    @Override
    public Object visitClassDecl(ClassDecl cd, IdentificationTable table) {
        // Record all members in the table
        table.memberMs = new HashMap<>();
        table.memberFs = new HashMap<>();
        for (FieldDecl field : cd.fieldDeclList) {
            if (table.memberFs.containsKey(field.name)) {
                throw error("Identification error - duplicate field declaration", field.posn.line);
            }
            table.memberFs.put(field.name, field);
        }
        for (MethodDecl method : cd.methodDeclList) {
            if (table.memberMs.containsKey(method.name)) {
                throw error("Identification error - duplicate method declaration",
                        method.posn.line);
            }
            table.memberMs.put(method.name, method);
        }

        // Link each field to the matching declaration
        for (FieldDecl field : cd.fieldDeclList) {
            field.visit(this, table);
        }

        // Perform contextual analysis on each method
        for (MethodDecl method : cd.methodDeclList) {
            method.visit(this, table);
        }

        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, IdentificationTable table) {
        // Visit the TypeDenoter
        fd.type.visit(this, table);
        return null;
    }

    // TODO use this
    private TypeDenoter expectedRetType;

    @Override
    public Object visitMethodDecl(MethodDecl md, IdentificationTable table) {
        // Add proper maps to table, visit parameter Objects
        table.params = new HashMap<>();
        for (ParameterDecl param : md.parameterDeclList) {
            if (table.params.containsKey(param.name)) {
                throw error("Identification error - duplicate parameter name", param.posn.line);
            }
            param.visit(this, table);
            table.params.put(param.name, param);
        }
        table.locals.push(new HashMap<>());

        // Visit & record expected return type
        md.type.visit(this, table);
        expectedRetType = md.getType();

        // Check each statement
        for (Statement s : md.statementList) {
            s.visit(this, table);
        }

        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, IdentificationTable table) {
        // Visit the parameter's TypeDenoter
        pd.type.visit(this, table);

        return null;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, IdentificationTable table) {
        // Visit the declaration's TypeDenoter
        decl.type.visit(this, table);

        // Add this declaration to the table
        if (table.locals.peek().containsKey(decl.name)) {
            throw error("Identification error - duplicate local variable name", decl.posn.line);
        }
        if (table.params.containsKey(decl.name)) {
            throw error(
                    "Identification error - local variable conflicts with parameter declaration",
                    decl.posn.line);
        }
        table.locals.peek().put(decl.name, decl);

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
        // Find corresponding class in the the table
        type.setDecl(table.classes.get(type.className));

        // If not found, record a nonfatal error and make the type ERROR
        if (type.getDecl() == null) {
            error("Type error - unknown class name", type.posn.line);
            type.typeKind = TypeKind.ERROR;
        }

        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, IdentificationTable table) {
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, IdentificationTable table) {
        // TODO check for match with expectedRetVal

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNullExpr(NullExpr expr, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitThisRef(ThisRef ref, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier id, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitOperator(Operator op, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, IdentificationTable table) {
        // TODO Auto-generated method stub
        return null;
    }

}
