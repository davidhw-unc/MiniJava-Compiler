package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NullExpr extends Expression {

    // TODO make sure this is only found in appropriate places

    public NullExpr(SourcePosition posn) {
        super(posn);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitNullExpr(this, o);
    }

}
