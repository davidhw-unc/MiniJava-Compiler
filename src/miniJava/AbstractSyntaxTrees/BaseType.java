/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter {
    private static final BaseType boolType = new BaseType(TypeKind.BOOLEAN);
    private static final BaseType intType = new BaseType(TypeKind.INT);
    private static final BaseType voidType = new BaseType(TypeKind.VOID);
    private static final BaseType unsupportedType = new BaseType(TypeKind.UNSUPPORTED);
    private static final BaseType errorType = new BaseType(TypeKind.ERROR);

    public static BaseType getType(TypeKind t) {
        switch (t) {
            case BOOLEAN:
                return boolType;
            case INT:
                return intType;
            case VOID:
                return voidType;
            case UNSUPPORTED:
                return unsupportedType;
            case ERROR:
                return errorType;
            default:
                throw new IllegalArgumentException(t + " is not a valid TypeKind for a BaseType");
        }
    }

    private BaseType(TypeKind t) {
        super(t, new SourcePosition(0, 0));
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitBaseType(this, o);
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return super.equals(obj);
    }
}
