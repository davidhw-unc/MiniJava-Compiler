/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class UnaryExpr extends Expression {
    public Operator operator;
    public Expression operandExpr;

    public UnaryExpr(Operator o, Expression e, SourcePosition posn) {
        super(posn);
        operator = o;
        operandExpr = e;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitUnaryExpr(this, o);
    }
}