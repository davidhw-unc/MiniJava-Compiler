package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class TernaryExpr extends Expression {
    public Expression leftExpr;
    public Expression midExpr;
    public Expression rightExpr;

    public Operator operator;

    public TernaryExpr(Operator operator, Expression leftExpr, Expression midExpr,
            Expression rightExpr, SourcePosition posn) {
        super(posn);
        this.leftExpr = leftExpr;
        this.midExpr = midExpr;
        this.rightExpr = rightExpr;
        this.operator = operator;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitTernaryExpr(this, o);
    }
}
