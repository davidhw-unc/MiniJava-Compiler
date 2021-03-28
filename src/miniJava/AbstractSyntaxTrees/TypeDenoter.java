/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST implements Typed {
    public TypeKind typeKind;

    public TypeDenoter(TypeKind type, SourcePosition posn) {
        super(posn);
        typeKind = type;
    }

    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        Typed.validateTypeCount(0, types);
        return this;
    }

    public abstract boolean eq(TypeDenoter other);

    public static TypeDenoter getTypeFromEq(TypeDenoter a, TypeDenoter b) {
        return a.eq(b) ? a : BaseType.getType(TypeKind.ERROR);
    }
}
