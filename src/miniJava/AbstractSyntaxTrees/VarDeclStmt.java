/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class VarDeclStmt extends Statement {
    public VarDecl varDecl;
    public Expression initExp;

    public VarDeclStmt(VarDecl vd, Expression e, SourcePosition posn) {
        super(posn);
        varDecl = vd;
        initExp = e;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitVarDeclStmt(this, o);
    }
}
