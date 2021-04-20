/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IxExpr extends Expression {
    public Reference ref;
    public Expression ixExpr;

    public IxExpr(Reference r, Expression e, SourcePosition posn) {
        super(posn);
        ref = r;
        ixExpr = e;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIxExpr(this, o);
    }

    // TODO clean up
    // Even if I undelete these, DO NOT restore this one... it's very wrong 
    /*
    @Override
    public boolean isKnown() {
        return ixExpr.isKnown() && ref instanceof IdRef && ref.getId().getDecl() instanceof VarDecl
                && ((VarDecl) ref.getId().getDecl()).getValue() != null;
    }
    */
}
