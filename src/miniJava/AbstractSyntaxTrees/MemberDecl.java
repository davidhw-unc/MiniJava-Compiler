/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class MemberDecl extends Declaration {
    public final boolean isPrivate;
    public final boolean isStatic;

    public MemberDecl(boolean isPrivate, boolean isStatic, TypeDenoter mt, String name,
            SourcePosition posn) {
        super(name, mt, posn);
        this.isPrivate = isPrivate;
        this.isStatic = isStatic;
    }

    public MemberDecl(MemberDecl md, SourcePosition posn) {
        super(md.name, md.getType(), posn);
        this.isPrivate = md.isPrivate;
        this.isStatic = md.isStatic;
    }
}
