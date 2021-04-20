/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class CallExpr extends Expression implements MethodCaller {
    private Reference methodRef;
    private ExprList argList;

    public CallExpr(Reference f, ExprList el, SourcePosition posn) {
        super(posn);
        methodRef = f;
        argList = el;
    }

    @Override
    public Reference getMethodRef() {
        return methodRef;
    }

    @Override
    public ExprList getArgList() {
        return argList;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitCallExpr(this, o);
    }

    // TODO clean up
    /*
    @Override
    public boolean isKnown() {
        return false;
    }
    */
}