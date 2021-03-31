/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class BooleanLiteral extends Literal {
    public BooleanLiteral(Token t) {
        super(t, new BaseType(TypeKind.BOOLEAN, t.posn));
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitBooleanLiteral(this, o);
    }
}
