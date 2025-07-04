/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IfStmt extends Statement {
    public Expression condExpr;
    public Statement thenStmt;
    public Statement elseStmt;

    public IfStmt(Expression b, Statement t, Statement e, SourcePosition posn) {
        super(posn);
        condExpr = b;
        thenStmt = t;
        elseStmt = e;
    }

    public IfStmt(Expression b, Statement t, SourcePosition posn) {
        super(posn);
        condExpr = b;
        thenStmt = t;
        elseStmt = null;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIfStmt(this, o);
    }
}