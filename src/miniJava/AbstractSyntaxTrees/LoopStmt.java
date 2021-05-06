/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

// This is a generalized AST node that represents both for loops and while loops
// While loops just have 
public class LoopStmt extends Statement {
    // initList is only allowed to contain AssignStmt, CallStmt, or IxAssignStmt, but its up to the
    // parser to verify that
    // Also- initList and initSingle will never both be non-null, but they can both be null
    private final StatementList initList;
    private final VarDeclStmt initDecl;
    public Expression condExpr;
    public Statement body;
    // The update section is simply combined into the end of the body via a wrapper BlockStmt
    // This is done in the parser!

    private LoopStmt(StatementList initList, VarDeclStmt initDecl, Expression condExpr,
            Statement body, SourcePosition posn) {
        super(posn);
        this.initList = initList;
        this.initDecl = initDecl;
        this.condExpr = condExpr;
        this.body = body;
    }

    /**
     * Represents a for loop with a list of statements for the initializer
     * 
     * @param initList
     * @param condExpr
     * @param body
     * @param posn
     */
    public LoopStmt(StatementList initList, Expression condExpr, Statement body,
            SourcePosition posn) {
        this(initList, null, condExpr, body, posn);
    }

    /**
     * Represents a for loop with a single variable declaration for the initializer
     * 
     * @param initDecl
     * @param condExpr
     * @param body
     * @param posn
     */
    public LoopStmt(VarDeclStmt initDecl, Expression condExpr, Statement body,
            SourcePosition posn) {
        this(null, initDecl, condExpr, body, posn);
    }

    /**
     * Represents a for loop with an empty initializer or a while loop
     * 
     * @param condExpr
     * @param body
     * @param posn
     */
    public LoopStmt(Expression condExpr, Statement body,
            SourcePosition posn) {
        this(null, null, condExpr, body, posn);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitLoopStmt(this, o);
    }

    public StatementList getInitList() {
        return initList;
    }

    public VarDeclStmt getInitDecl() {
        return initDecl;
    }
}
