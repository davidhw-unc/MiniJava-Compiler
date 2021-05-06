/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassType extends TypeDenoter {
    public String className;

    private ClassDecl decl;

    public ClassType(String cn, SourcePosition posn) {
        super(TypeKind.CLASS, posn);
        className = cn;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitClassType(this, o);
    }

    public ClassDecl getDecl() {
        return decl;
    }

    public void setDecl(ClassDecl decl) {
        this.decl = decl;
    }

    @Override
    public String toString() {
        return className;
    }
}
