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

    private ClassType type;

    public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
        super(cn, null, posn);
        fieldDeclList = fdl;
        methodDeclList = mdl;
        type = new ClassType(cn);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitClassDecl(this, o);
    }

    @Override
    public TypeDenoter getType() {
        return type;
    }

    @Override
    boolean hasBeenAnalyzed() {
        // TODO Auto-generated method stub
        return false;
    }
}
