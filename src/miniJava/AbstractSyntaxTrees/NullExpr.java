package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NullExpr extends Expression {
    public NullExpr(SourcePosition posn) {
        super(posn);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitNullExpr(this, o);
    }

    // TODO clean up
    /*
    @Override
    public boolean isKnown() {
        return true;
    }
    */
}
