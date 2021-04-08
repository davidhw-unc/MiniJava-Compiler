package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class CodeGenerator implements Visitor<Object, Object> {

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitPackage(Package prog, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitVarDeclStmt(VarDeclStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNullExpr(NullExpr expr, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

}
