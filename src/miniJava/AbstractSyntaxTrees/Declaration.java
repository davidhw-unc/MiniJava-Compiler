/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST implements Typed {
    public String name;
    /**
     * Used for code generation <br>
     * ClassDecl: number of fields (pass 1) <br>
     * FieldDecl (non-static): offset relative to OB (pass 1) <br>
     * FieldDecl (static): offset relative to SB (pass 1) <br>
     * ParameterDecl: offset relative to LB (should be negative) (pass 2) <br>
     * VarDecl: offset relative to LB (should be positive) (pass 2) <br>
     * MethodDecl: offset relative to CB (pass 2,
     */
    public int data = Integer.MIN_VALUE;

    private TypeDenoter type;

    public Declaration(String name, TypeDenoter type, SourcePosition posn) {
        super(posn);
        this.name = name;
        this.type = type;
    }

    @Override
    public TypeDenoter getType() {
        return type;
    }

    @Override
    public void setType(TypeDenoter type) {
        throw new UnsupportedOperationException("Cannot change the type of a declaration");
    }
}
