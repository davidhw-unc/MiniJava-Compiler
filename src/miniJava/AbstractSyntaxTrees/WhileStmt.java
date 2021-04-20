/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class WhileStmt extends Statement {
    public Expression condExpr;
    public Statement body;

    public WhileStmt(Expression e, Statement s, SourcePosition posn) {
        super(posn);
        condExpr = e;
        body = s;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitWhileStmt(this, o);
    }
}
