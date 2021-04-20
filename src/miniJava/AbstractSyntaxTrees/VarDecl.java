/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class VarDecl extends LocalDecl {
    private Integer value = 0;

    public VarDecl(TypeDenoter t, String name, SourcePosition posn) {
        super(name, t, posn);
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    /**
     * Retrieves the current value of this variable if it can be known at compile time.
     * 
     * @return null if the current value isn't known at compile time, Integer representation of the
     *         current value otherwise. If this variable points to an object, the current value will
     *         never be known at compile time.
     */
    public Integer getValue() {
        return value;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitVarDecl(this, o);
    }
}
