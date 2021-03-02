/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class AST {

    public AST(SourcePosition posn) {
        this.posn = posn;
    }

    @Override
    public String toString() {
        String fullClassName = this.getClass().getName();
        StringBuilder cn = new StringBuilder(fullClassName.substring(1 + fullClassName.lastIndexOf('.')));
        if (ASTDisplay.showPosition) cn.append(" ").append(posn.toString());
        return cn.toString();
    }

    public abstract <A, R> R visit(Visitor<A, R> v, A o);

    public SourcePosition posn;
}
