/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class Identifier extends Terminal {
    private Declaration decl;

    public Identifier(Token t) {
        super(t);
    }

    public Declaration getDecl() {
        return decl;
    }

    protected void setDecl(Declaration decl) {
        this.decl = decl;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIdentifier(this, o);
    }

    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        if (decl != null) {
            return decl.getAndCheckType(types);
        }
        throw new IllegalStateException("Declaration corresponding to the Identifier at " + posn
                + " has not yet been assigned");
    }
}
