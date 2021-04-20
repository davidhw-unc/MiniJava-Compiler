/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class ThisRef extends BaseRef {
    public ThisRef(Token token) {
        super(token.posn);
        super.setId(new Identifier(token));
    }

    @Override
    public void setId(Identifier id) {
        throw new UnsupportedOperationException("Cannot set the Identifier of a ThisRef");
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitThisRef(this, o);
    }
}
