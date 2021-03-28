/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends Declaration {
    public FieldDeclList fieldDeclList;
    public MethodDeclList methodDeclList;

    public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
        super(cn, null, posn); // starts out with null type
        fieldDeclList = fdl;
        methodDeclList = mdl;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitClassDecl(this, o);
    }

    protected void setType(ClassType type) {
        this.type = type;
    }
}
