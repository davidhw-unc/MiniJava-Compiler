/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Expression extends AST implements Typed {
    private TypeDenoter type = null;

    // TODO clean up
    // It only makes sense to call this while performing the code generation traversal
    // Otherwise, any Expression containing references to local fields may give incorrect results
    //public abstract boolean isKnown();

    public Expression(SourcePosition posn) {
        super(posn);
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
