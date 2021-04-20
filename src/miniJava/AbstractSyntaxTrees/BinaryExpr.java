/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BinaryExpr extends Expression {
    public Operator operator;
    public Expression leftExpr;
    public Expression rightExpr;

    public BinaryExpr(Operator o, Expression e1, Expression e2, SourcePosition posn) {
        super(posn);
        operator = o;
        leftExpr = e1;
        rightExpr = e2;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitBinaryExpr(this, o);
    }

    // TODO clean up
    /*
    @Override
    public boolean isKnown() {
        // If both operand values are known, this expression is always known
        return left.isKnown() && right.isKnown();
    }
    */
}