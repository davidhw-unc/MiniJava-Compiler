/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class Identifier extends Terminal implements Typed {
    private Declaration decl = null;
    private TypeDenoter type = null;

    public Identifier(Token t) {
        super(t);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIdentifier(this, o);
    }

    public Declaration getDecl() {
        return decl;
    }

    public void setDecl(Declaration decl) {
        this.decl = decl;
    }

    @Override
    public TypeDenoter getType() {
        return type;
    }

    @Override
    public void setType(TypeDenoter type) {
        this.type = type;
    }
}
