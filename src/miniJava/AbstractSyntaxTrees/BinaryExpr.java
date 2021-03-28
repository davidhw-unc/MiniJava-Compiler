/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BinaryExpr extends Expression {
    public Operator operator;
    public Expression left;
    public Expression right;

    public BinaryExpr(Operator o, Expression e1, Expression e2, SourcePosition posn) {
        super(posn);
        operator = o;
        left = e1;
        right = e2;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitBinaryExpr(this, o);
    }

    TypeDenoter type = null;

    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        if (type == null) {
            type = TypeDenoter.getTypeFromEq(left.getAndCheckType(types), right.getAndCheckType(types));
            if (type.typeKind == TypeKind.ERROR) {

            }
        }
    }
}