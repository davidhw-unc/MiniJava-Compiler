/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class Operator extends Terminal {
    private final int operandCount;

    public Operator(Token t, int operandCount) {
        super(t);
        this.operandCount = operandCount;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitOperator(this, o);
    }

    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        return OperationType.getType(kind, operandCount).getAndCheckType(types);
    }
}
