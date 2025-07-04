/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class LiteralExpr extends Expression {
    public Literal lit;

    public LiteralExpr(Literal t, SourcePosition posn) {
        super(t.posn);
        lit = t;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitLiteralExpr(this, o);
    }
}