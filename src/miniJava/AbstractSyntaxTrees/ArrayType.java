/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {
    public TypeDenoter eltType;

    public ArrayType(TypeDenoter eltType, SourcePosition posn) {
        super(TypeKind.ARRAY, posn);
        this.eltType = eltType;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitArrayType(this, o);
    }

    @Override
    public String toString() {
        return eltType.toString() + "[]";
    }
}
