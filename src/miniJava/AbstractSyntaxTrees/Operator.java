/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class Operator extends Terminal {
    public Operator(Token t) {
        super(t);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitOperator(this, o);
    }

    @Override
    public TypeDenoter getType() {
        return null;
    }

    @Override
    boolean hasBeenAnalyzed() {
        return true;
    }
}
