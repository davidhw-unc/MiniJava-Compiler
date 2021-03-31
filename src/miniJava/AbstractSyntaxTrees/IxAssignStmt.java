/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IxAssignStmt extends Statement {
    public Reference ref;
    public Expression ixExpr;
    public Expression exp;

    public IxAssignStmt(Reference r, Expression i, Expression e, SourcePosition posn) {
        super(posn);
        ref = r;
        ixExpr = i;
        exp = e;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIxAssignStmt(this, o);
    }
}
