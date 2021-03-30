/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisRef extends BaseRef {
    public ThisRef(SourcePosition posn) {
        super(posn);
    }

    // TODO clean up
    /*
    private ClassDecl decl;
    
    public Declaration getDecl() {
        return decl;
    }
    
    protected void setDecl(ClassDecl decl) {
        this.decl = decl;
    }
    */

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitThisRef(this, o);
    }
}
