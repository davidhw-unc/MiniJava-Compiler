/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Reference extends AST implements Typed {
    private Declaration decl;

    public Reference(SourcePosition posn) {
        super(posn);
    }

    public Declaration getDecl() {
        return decl;
    }

    protected void setDecl(Declaration decl) {
        this.decl = decl;
    }
}
