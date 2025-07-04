/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

/*
 * Display AST in text form, one node per line, using indentation to show 
 * subordinate nodes below a parent node.
 *   
 * Performs an in-order traversal of AST, visiting an AST node of type XXX 
 * with a method of the form  
 *   
 *       public Object visitXXX( XXX astnode, String arg)
 *       
 *   where arg is a prefix string (indentation) to precede display of ast node
 *   and a null Object is returned as the result.
 *   The display is produced by printing a line of output at each node visited.
 */
public class ASTDisplay implements Visitor<String, Object> {

    public static boolean showPosition = false;
    public static boolean showTypes = false;

    /**
     * print text representation of AST to stdout
     * 
     * @param ast root node of AST
     */
    public void showTree(AST ast) {
        System.out.println("======= AST Display =========================");
        ast.visit(this, "");
        System.out.println("=============================================");
    }

    // methods to format output

    /**
     * display arbitrary text for a node
     * 
     * @param prefix indent text to indicate depth in AST
     * @param text   preformatted node display
     */
    private void show(String prefix, String text) {
        System.out.println(prefix + text);
    }

    /**
     * display AST node by name
     * 
     * @param prefix spaced indent to indicate depth in AST
     * @param node   AST node, will be shown by name
     */
    private void show(String prefix, AST node) {
        System.out.println(prefix + node.toString());
    }

    /**
     * quote a string
     * 
     * @param text string to quote
     */
    private String quote(String text) {
        return ("\"" + text + "\"");
    }

    /**
     * increase depth in AST
     * 
     * @param prefix current spacing to indicate depth in AST
     * @return new spacing
     */
    private String indent(String prefix) {
        return prefix + "  ";
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitPackage(Package prog, String arg) {
        show(arg, prog);
        ClassDeclList cl = prog.classDeclList;
        show(arg, "  ClassDeclList [" + cl.size() + "]");
        String pfx = arg + "  . ";
        for (ClassDecl c : prog.classDeclList) {
            c.visit(this, pfx);
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitClassDecl(ClassDecl clas, String arg) {
        show(arg, clas);
        show(indent(arg), quote(clas.name) + " classname");
        show(arg, "  FieldDeclList [" + clas.fieldDeclList.size() + "]");
        String pfx = arg + "  . ";
        for (FieldDecl f : clas.fieldDeclList)
            f.visit(this, pfx);
        show(arg, "  MethodDeclList [" + clas.methodDeclList.size() + "]");
        for (MethodDecl m : clas.methodDeclList)
            m.visit(this, pfx);
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl f, String arg) {
        show(arg, "(" + (f.isPrivate ? "private" : "public") + (f.isStatic ? " static) " : ") ")
                + f.toString());
        f.getType().visit(this, indent(arg));
        show(indent(arg), quote(f.name) + " fieldname");
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl m, String arg) {
        show(arg, "(" + (m.isPrivate ? "private" : "public") + (m.isStatic ? " static) " : ") ")
                + m.toString());
        m.getType().visit(this, indent(arg));
        show(indent(arg), quote(m.name) + " methodname");
        ParameterDeclList pdl = m.parameterDeclList;
        show(arg, "  ParameterDeclList [" + pdl.size() + "]");
        String pfx = (arg) + "  . ";
        for (ParameterDecl pd : pdl) {
            pd.visit(this, pfx);
        }
        StatementList sl = m.statementList;
        show(arg, "  StmtList [" + sl.size() + "]");
        for (Statement s : sl) {
            s.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, String arg) {
        show(arg, pd);
        pd.getType().visit(this, indent(arg));
        show(indent(arg), quote(pd.name) + "parametername ");
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl vd, String arg) {
        show(arg, vd);
        vd.getType().visit(this, indent(arg));
        show(indent(arg), quote(vd.name) + " varname");
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBaseType(BaseType type, String arg) {
        show(arg, type.typeKind + " " + type.toString());
        return null;
    }

    @Override
    public Object visitClassType(ClassType ct, String arg) {
        show(arg, ct);
        // Add "Identifier" to make PA2 checker happy 
        show(indent(arg), quote(ct.className) + " Identifier");
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, String arg) {
        show(arg, type);
        type.eltType.visit(this, indent(arg));
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBlockStmt(BlockStmt stmt, String arg) {
        show(arg, stmt);
        StatementList sl = stmt.sl;
        show(arg, "  StatementList [" + sl.size() + "]");
        String pfx = arg + "  . ";
        for (Statement s : sl) {
            s.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitVarDeclStmt(VarDeclStmt stmt, String arg) {
        show(arg, stmt);
        stmt.varDecl.visit(this, indent(arg));
        stmt.initExp.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, String arg) {
        show(arg, stmt);
        stmt.ref.visit(this, indent(arg));
        stmt.valExpr.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, String arg) {
        show(arg, stmt);
        stmt.ref.visit(this, indent(arg));
        stmt.ixExpr.visit(this, indent(arg));
        stmt.valExp.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, String arg) {
        show(arg, stmt);
        stmt.getMethodRef().visit(this, indent(arg));
        ExprList al = stmt.getArgList();
        show(arg, "  ExprList [" + al.size() + "]");
        String pfx = arg + "  . ";
        for (Expression e : al) {
            e.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, String arg) {
        show(arg, stmt);
        if (stmt.returnExpr != null) stmt.returnExpr.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, String arg) {
        show(arg, stmt);
        stmt.condExpr.visit(this, indent(arg));
        stmt.thenStmt.visit(this, indent(arg));
        if (stmt.elseStmt != null) stmt.elseStmt.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitLoopStmt(LoopStmt stmt, String arg) {
        show(arg, stmt);
        stmt.condExpr.visit(this, indent(arg));
        stmt.body.visit(this, indent(arg));
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.operator.visit(this, indent(arg));
        expr.operandExpr.visit(this, indent(indent(arg)));
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.operator.visit(this, indent(arg));
        expr.leftExpr.visit(this, indent(indent(arg)));
        expr.rightExpr.visit(this, indent(indent(arg)));
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.ref.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.ref.visit(this, indent(arg));
        expr.ixExpr.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.getMethodRef().visit(this, indent(arg));
        ExprList al = expr.getArgList();
        show(arg, "  ExprList + [" + al.size() + "]");
        String pfx = arg + "  . ";
        for (Expression e : al) {
            e.visit(this, pfx);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.lit.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.eltType.visit(this, indent(arg));
        expr.sizeExpr.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        expr.classtype.visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitNullExpr(NullExpr expr, String arg) {
        show(arg, expr);
        if (showTypes) {
            expr.getType().visit(this, indent(arg));
        }
        return null;
    }

    @Override
    public Object visitTernaryExpr(TernaryExpr te, String arg) {
        show(arg, te);
        if (showTypes) {
            te.getType().visit(this, indent(arg));
        }
        te.operator.visit(this, indent(arg));
        te.leftExpr.visit(this, indent(indent(arg)));
        te.midExpr.visit(this, indent(indent(arg)));
        te.rightExpr.visit(this, indent(indent(arg)));
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitThisRef(ThisRef ref, String arg) {
        show(arg, ref);
        if (showTypes) {
            ref.getType().visit(this, indent(arg));
        }
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, String arg) {
        show(arg, ref);
        if (showTypes) {
            ref.getType().visit(this, indent(arg));
        }
        ref.getId().visit(this, indent(arg));
        return null;
    }

    @Override
    public Object visitQualRef(QualRef ref, String arg) {
        show(arg, ref);
        if (showTypes) {
            ref.getType().visit(this, indent(arg));
        }
        ref.getId().visit(this, indent(arg));
        ref.prevRef.visit(this, indent(arg));
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier id, String arg) {
        show(arg, quote(id.spelling) + " " + id.toString());
        if (showTypes) {
            id.getType().visit(this, indent(arg));
        }
        return null;
    }

    @Override
    public Object visitOperator(Operator op, String arg) {
        show(arg, quote(op.spelling) + " " + op.toString());
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, String arg) {
        show(arg, quote(num.spelling) + " " + num.toString());
        if (showTypes) {
            num.getType().visit(this, indent(arg));
        }
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, String arg) {
        show(arg, quote(bool.spelling) + " " + bool.toString());
        if (showTypes) {
            bool.getType().visit(this, indent(arg));
        }
        return null;
    }
}
