/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class RefExpr extends Expression {
    public Reference ref;

    public RefExpr(Reference r, SourcePosition posn) {
        super(posn);
        ref = r;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitRefExpr(this, o);
    }
}
