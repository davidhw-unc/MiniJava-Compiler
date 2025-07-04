/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Expression extends AST implements Typed {
    private TypeDenoter type = null;

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
