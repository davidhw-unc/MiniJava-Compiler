/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NewArrayExpr extends NewExpr {
    public TypeDenoter eltType;
    public Expression sizeExpr;

    public NewArrayExpr(TypeDenoter et, Expression e, SourcePosition posn) {
        super(posn);
        eltType = et;
        sizeExpr = e;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitNewArrayExpr(this, o);
    }
}