/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisRef extends BaseRef {
    private ClassDecl decl;

    public ThisRef(SourcePosition posn) {
        super(posn);
    }

    public Declaration getDecl() {
        return decl;
    }

    protected void setDecl(ClassDecl decl) {
        this.decl = decl;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitThisRef(this, o);
    }

    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        if (decl != null) {
            return decl.getAndCheckType(types);
        }
        throw new IllegalStateException("Declaration corresponding to the ThisRef at " + posn
                + " has not yet been assigned");
    }
}
