/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {
    public TypeKind typeKind;

    public TypeDenoter(TypeKind type, SourcePosition posn) {
        super(posn);
        typeKind = type;
    }

    // TODO clean up
    /*
    @Override
    public TypeDenoter getType() {
        return this;
    }
    
    @Override
    public void setType(TypeDenoter type) {
        throw new UnsupportedOperationException("Cannot change the type of a TypeDenoter");
    }
    */

    public static boolean eq(TypeDenoter a, TypeDenoter b) {
        // Deal with UNSUPPORTED (never equal)
        if (a.typeKind == TypeKind.UNSUPPORTED || b.typeKind == TypeKind.UNSUPPORTED) {
            return false;
        }

        // Deal with ERROR (always equal)
        if (a.typeKind == TypeKind.ERROR || b.typeKind == TypeKind.ERROR) {
            return true;
        }

        // Deal with VOID
        // Note: VOID is exclusively used for functions with void return
        // Therefore we can throw an error, as a void function's type shouldn't be compared
        // TODO account for the expression where such a function is called
        if (a.typeKind == TypeKind.VOID || b.typeKind == TypeKind.VOID) {
            throw new IllegalArgumentException("VOID types should never need to be compared");
        }

        // Deal with everything else
        if (a.typeKind != b.typeKind) {
            return false;
        }
        switch (a.typeKind) { // At this point we know a and b's typeKinds are the same
            case INT:
            case BOOLEAN:
                // These two are always BaseTypes
                return true;
            case CLASS:
                return ((ClassType) a).className.equals(((ClassType) b).className);
            case ARRAY:
                return TypeDenoter.eq(((ArrayType) a).eltType, ((ArrayType) b).eltType);
            default:
                throw new IllegalStateException("This should be impossible to reach!!!");
        }
    }
}
