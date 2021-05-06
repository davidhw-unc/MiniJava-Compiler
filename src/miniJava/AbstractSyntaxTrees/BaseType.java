/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter {
    public static final BaseType bool_dummy = new BaseType(TypeKind.BOOLEAN, null);
    public static final BaseType int_dummy = new BaseType(TypeKind.INT, null);
    public static final BaseType void_dummy = new BaseType(TypeKind.VOID, null);
    public static final BaseType unsupported_dummy = new BaseType(TypeKind.UNSUPPORTED, null);
    public static final BaseType error_dummy = new BaseType(TypeKind.ERROR, null);

    public BaseType(TypeKind t, SourcePosition posn) {
        super(t, posn);
        if (t == TypeKind.CLASS || t == TypeKind.ARRAY) {
            throw new IllegalArgumentException(t + " is not a valid TypeKind for a BaseType");
        }
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitBaseType(this, o);
    }

    @Override
    public String toString() {
        switch (typeKind) {
            case BOOLEAN:
                return "boolean";
            case ERROR:
                return "error";
            case INT:
                return "int";
            case UNSUPPORTED:
                return "unsupported";
            case VOID:
                return "void";
            default:
                throw new IllegalStateException(
                        "Tried to convert a BaseType with an illegal TypeKind to a string");
        }
    }
}
