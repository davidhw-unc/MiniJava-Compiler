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
        if (decl != null) {
            return decl;
        }
        throw new UnsupportedOperationException(
                "ClassType on line " + posn.line + " has not yet been assigned a declaration");
    }

    public void setDecl(ClassDecl decl) {
        this.decl = decl;
    }
}
