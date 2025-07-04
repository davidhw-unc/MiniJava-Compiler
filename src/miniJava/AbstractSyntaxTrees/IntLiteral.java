/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class IntLiteral extends Literal {
    public IntLiteral(Token t) {
        super(t, new BaseType(TypeKind.INT, t.posn));
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIntLiteral(this, o);
    }
}
