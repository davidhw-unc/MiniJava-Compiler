/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Reference extends AST implements Typed {
    public Reference(SourcePosition posn) {
        super(posn);
    }

    /*
    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        Typed.validateTypeCount(0, types);
        if (decl != null) {
            return decl.getAndCheckType(types);
        }
        throw new IllegalStateException("Declaration corresponding to the Reference at " + posn
                + " has not yet been assigned");
    }
    */
}
