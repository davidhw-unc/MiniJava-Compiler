/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

/**
 * An implementation of the Visitor interface provides a method visitX for each
 * non-abstract AST class X.
 */
public interface Visitor<ArgType, ResultType> {

    // Package
    public ResultType visitPackage(Package prog, ArgType arg);

    // Declarations
    public ResultType visitClassDecl(ClassDecl cd, ArgType arg);

    public ResultType visitFieldDecl(FieldDecl fd, ArgType arg);

    public ResultType visitMethodDecl(MethodDecl md, ArgType arg);

    public ResultType visitParameterDecl(ParameterDecl pd, ArgType arg);

    public ResultType visitVarDecl(VarDecl vd, ArgType arg);

    // Types
    public ResultType visitBaseType(BaseType bt, ArgType arg);

    public ResultType visitClassType(ClassType ct, ArgType arg);

    public ResultType visitArrayType(ArrayType at, ArgType arg);

    // Statements
    public ResultType visitBlockStmt(BlockStmt bs, ArgType arg);

    public ResultType visitVarDeclStmt(VarDeclStmt vds, ArgType arg);

    public ResultType visitAssignStmt(AssignStmt as, ArgType arg);

    public ResultType visitIxAssignStmt(IxAssignStmt ias, ArgType arg);

    public ResultType visitCallStmt(CallStmt cs, ArgType arg);

    public ResultType visitReturnStmt(ReturnStmt rs, ArgType arg);

    public ResultType visitIfStmt(IfStmt is, ArgType arg);

    public ResultType visitLoopStmt(LoopStmt ls, ArgType arg);

    // Expressions
    public ResultType visitUnaryExpr(UnaryExpr ue, ArgType arg);

    public ResultType visitBinaryExpr(BinaryExpr be, ArgType arg);

    public ResultType visitTernaryExpr(TernaryExpr te, ArgType arg);

    public ResultType visitRefExpr(RefExpr re, ArgType arg);

    public ResultType visitIxExpr(IxExpr ie, ArgType arg);

    public ResultType visitCallExpr(CallExpr ce, ArgType arg);

    public ResultType visitLiteralExpr(LiteralExpr le, ArgType arg);

    public ResultType visitNewObjectExpr(NewObjectExpr noe, ArgType arg);

    public ResultType visitNewArrayExpr(NewArrayExpr nae, ArgType arg);

    public ResultType visitNullExpr(NullExpr ne, ArgType arg);

    // References
    public ResultType visitThisRef(ThisRef tr, ArgType arg);

    public ResultType visitIdRef(IdRef ir, ArgType arg);

    public ResultType visitQualRef(QualRef qr, ArgType arg);

    // Terminals
    public ResultType visitIdentifier(Identifier i, ArgType arg);

    public ResultType visitOperator(Operator o, ArgType arg);

    public ResultType visitIntLiteral(IntLiteral il, ArgType arg);

    public ResultType visitBooleanLiteral(BooleanLiteral bl, ArgType arg);
}
