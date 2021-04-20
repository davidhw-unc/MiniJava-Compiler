package miniJava.SyntacticAnalyzer;

import static miniJava.SyntacticAnalyzer.Token.Kind.*;

import java.util.Arrays;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token.Kind;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
    private Scanner scan;
    private ErrorReporter reporter;

    public static boolean trace = false;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scan = scanner;
        this.reporter = reporter;
    }

    private static class SyntaxException extends Exception {
        private static final long serialVersionUID = -5685400912669788293L;
    }

    public AST parse() {
        try {
            return parseProgram();
        } catch (SyntaxException e) {
            return null;
        }
    }

    /*----------------------------------------------------*
     * Parsing methods                                    *
     *----------------------------------------------------*/

    private Package parseProgram() throws SyntaxException {
        ClassDeclList cList = new ClassDeclList();
        SourcePosition pos = scan.peek().posn;
        while (acceptOpt(EOT) == null) {
            cList.add(parseClassDec());
        }
        return new Package(cList, pos);
    }

    private ClassDecl parseClassDec() throws SyntaxException {
        SourcePosition posn = scan.peek().posn; // Get posn from this
        accept(CLASS);
        Token id = scan.peek();
        accept(ID);
        accept(LBRACE);
        FieldDeclList fields = new FieldDeclList();
        MethodDeclList methods = new MethodDeclList();
        while (acceptOpt(RBRACE) == null) {
            MemberDecl member = parseMember();
            if (member instanceof FieldDecl) {
                fields.add((FieldDecl) member);
            } else {
                methods.add((MethodDecl) member);
            }
        }
        return new ClassDecl(id.spelling, fields, methods, posn);
    }

    private MemberDecl parseMember() throws SyntaxException {
        boolean isPrivate = acceptOpt(PUBLIC, PRIVATE) == PRIVATE;
        boolean isStatic = acceptOpt(STATIC) == STATIC;
        Token prev = scan.peek();
        if (acceptOpt(VOID) != null) {
            Token id = scan.peek();
            accept(ID);
            return parseMethodDec(new FieldDecl(isPrivate, isStatic,
                    new BaseType(TypeKind.VOID, prev.posn), id.spelling, prev.posn));
        } else {
            TypeDenoter type = parseType();
            Token id = scan.peek();
            accept(ID);
            MemberDecl member = new FieldDecl(isPrivate, isStatic, type, id.spelling, id.posn);
            if (acceptOpt(SEMICOLON) == null) {
                member = parseMethodDec(member);
            }
            return member;
        }
    }

    private MethodDecl parseMethodDec(MemberDecl md) throws SyntaxException {
        accept(LPAREN);
        ParameterDeclList pList = new ParameterDeclList();
        if (acceptOpt(RPAREN) == null) {
            Token paramStart = scan.peek();
            TypeDenoter type = parseType();
            Token id = scan.peek();
            accept(ID);
            pList.add(new ParameterDecl(type, id.spelling, paramStart.posn));
            while (acceptOpt(COMMA) != null) {
                type = parseType();
                id = scan.peek();
                accept(ID);
                pList.add(new ParameterDecl(type, id.spelling, paramStart.posn));
            }
            accept(RPAREN);
        }
        accept(LBRACE);
        StatementList sList = new StatementList();
        while (acceptOpt(RBRACE) == null) {
            sList.add(parseStatement());
        }
        return new MethodDecl(md, pList, sList, md.posn);
    }

    @SuppressWarnings("incomplete-switch")
    private TypeDenoter parseType() throws SyntaxException {
        Token typeToken = scan.peek();
        Kind kind = accept(INT, BOOLEAN, ID);
        TypeDenoter type = null;
        switch (kind) {
            case INT:
                type = new BaseType(TypeKind.INT, typeToken.posn);
                break;
            case BOOLEAN:
                type = new BaseType(TypeKind.BOOLEAN, typeToken.posn);
                break;
            case ID:
                type = new ClassType(typeToken.spelling, typeToken.posn);
                break;
        }
        if (kind != BOOLEAN) {
            if (acceptOpt(LBRACKET) != null) {
                accept(RBRACKET);
                type = new ArrayType(type, typeToken.posn);
            }
        }
        return type;
    }

    // Nullable (i.e. may consume no tokens and return an empty ExprList)
    private ExprList parseArgumentsForCall() throws SyntaxException {
        ExprList args = new ExprList();
        if (scan.peek().kind != RPAREN) {
            args.add(parseExpression());
            while (acceptOpt(COMMA) != null) {
                args.add(parseExpression());
            }
        }
        return args;
    }

    private Reference parseReference() throws SyntaxException {
        return parseReference(null);
    }

    @SuppressWarnings("incomplete-switch")
    private Reference parseReference(Reference startingRef) throws SyntaxException {
        Reference ref = startingRef;
        SourcePosition posn;
        if (ref == null) {
            Token original = scan.peek();
            posn = original.posn;
            switch (accept(ID, THIS)) {
                case ID:
                    ref = new IdRef(new Identifier(original), posn);
                    break;
                case THIS:
                    ref = new ThisRef(original);
                    break;
            }
        } else {
            posn = startingRef.posn;
        }
        while (acceptOpt(DOT) != null) {
            Token prev = scan.peek();
            accept(ID);
            ref = new QualRef(ref, new Identifier(prev), posn);
        }
        return ref;
    }

    @SuppressWarnings("incomplete-switch")
    private Statement parseStatement() throws SyntaxException {
        Token first = scan.peek();
        Statement stmt = null;
        Kind firstKind = acceptOpt(LBRACE, RETURN, IF, WHILE);
        if (firstKind == null) {
            switch (scan.peek().kind) {
                case INT:
                case BOOLEAN:
                    // Variable declaration of int, boolean, or int[]
                    TypeDenoter type = parseType();
                    Token id = scan.peek();
                    accept(ID);
                    accept(ASSIGN);
                    stmt = new VarDeclStmt(new VarDecl(type, id.spelling, first.posn),
                            parseExpression(), first.posn);
                    break;
                case THIS:
                    // Reference-based statements starting with "this"
                    Reference ref = parseReference();
                    if (acceptOpt(LPAREN) != null) {
                        // Function call
                        stmt = new CallStmt(ref, parseArgumentsForCall(), first.posn);
                        accept(RPAREN);
                    } else {
                        // Assignment
                        if (acceptOpt(LBRACKET) != null) {
                            // Array element assignment
                            Expression ixExpr = parseExpression();
                            accept(RBRACKET);
                            accept(ASSIGN);
                            stmt = new IxAssignStmt(ref, ixExpr, parseExpression(), first.posn);
                        } else {
                            // Normal assignment
                            accept(ASSIGN);
                            stmt = new AssignStmt(ref, parseExpression(), first.posn);
                        }
                    }
                    break;
                case ID:
                    accept(ID);

                    // First, check the two cases where we start with an ID followed by an LBRACKET
                    if (acceptOpt(LBRACKET) != null) {
                        // Can either be a Type or a Reference
                        if (acceptOpt(RBRACKET) != null) {
                            // Must be an object array Type
                            Token objID = scan.peek();
                            accept(ID);
                            TypeDenoter t = new ArrayType(new ClassType(first.spelling, first.posn),
                                    first.posn);
                            accept(ASSIGN);
                            stmt = new VarDeclStmt(new VarDecl(t, objID.spelling, first.posn),
                                    parseExpression(), first.posn);
                            break; // Leave the switch statement
                        } else {
                            // Must be a reference for an array assignment
                            Reference refr = new IdRef(new Identifier(first), first.posn);
                            Expression ixExpr = parseExpression();
                            accept(RBRACKET);
                            accept(ASSIGN);
                            stmt = new IxAssignStmt(refr, ixExpr, parseExpression(), first.posn);
                            break; // Leave the switch statement
                        }
                    }

                    Token potentialID = scan.peek();
                    if (acceptOpt(ID) != null) {
                        // Must have been a Type for declaring a new local variable
                        accept(ASSIGN);
                        stmt = new VarDeclStmt(
                                new VarDecl(new ClassType(first.spelling, first.posn),
                                        potentialID.spelling, first.posn),
                                parseExpression(), first.posn);
                        break; // Leave the switch statement
                    }

                    // Get the rest of the reference
                    Reference refr = new IdRef(new Identifier(first), first.posn);
                    if (scan.peek().kind == DOT) {
                        // If there's a dot operator, continue parsing the reference
                        refr = parseReference(refr);
                    }

                    // Handle the array assignment case
                    if (acceptOpt(LBRACKET) != null) {
                        Expression ixExpr = parseExpression();
                        accept(RBRACKET);
                        accept(ASSIGN);
                        stmt = new IxAssignStmt(refr, ixExpr, parseExpression(), first.posn);
                        break; // Leave the switch statement
                    }

                    // Handle the function call case
                    if (acceptOpt(LPAREN) != null) {
                        stmt = new CallStmt(refr, parseArgumentsForCall(), first.posn);
                        accept(RPAREN);
                        break; // Leave the switch statement
                    }

                    // If none of the special cases apply, it's just a regular variable assignment
                    accept(ASSIGN);
                    stmt = new AssignStmt(refr, parseExpression(), first.posn);
                    break;
            }
            accept(SEMICOLON);
        } else {
            switch (firstKind) {
                case LBRACE:
                    // Block statement
                    StatementList sList = new StatementList();
                    while (acceptOpt(RBRACE) == null) {
                        sList.add(parseStatement());
                    }
                    stmt = new BlockStmt(sList, first.posn);
                    break;
                case RETURN:
                    // Return statement
                    Expression expr = null;
                    if (acceptOpt(SEMICOLON) == null) {
                        expr = parseExpression();
                        accept(SEMICOLON);
                    }
                    stmt = new ReturnStmt(expr, first.posn);
                    break;
                case IF:
                    // If(/else) statement
                    // In ambiguous case, "else" goes with inner "if"
                    accept(LPAREN);
                    Expression cond = parseExpression();
                    accept(RPAREN);
                    Statement thenStmt = parseStatement();
                    if (acceptOpt(ELSE) != null) {
                        stmt = new IfStmt(cond, thenStmt, parseStatement(), first.posn);
                    } else {
                        stmt = new IfStmt(cond, thenStmt, first.posn);
                    }
                    break;
                case WHILE:
                    // While loop
                    accept(LPAREN);
                    Expression condi = parseExpression();
                    accept(RPAREN);
                    stmt = new WhileStmt(condi, parseStatement(), first.posn);
                    break;
            }
        }
        return stmt;
    }

    // ------------------
    // Expression parsing
    // ------------------

    // || (binary)
    private Expression parseExpression() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        while (acceptOpt(OR) != null) {
            expr = new BinaryExpr(new Operator(oper, 2), expr, parseExprF(), start.posn);
            oper = scan.peek();
        }
        return expr;
    }

    // && (binary)
    private Expression parseExprF() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprG();
        Token oper = scan.peek();
        while (acceptOpt(AND) != null) {
            expr = new BinaryExpr(new Operator(oper, 2), expr, parseExprG(), start.posn);
            oper = scan.peek();
        }
        return expr;
    }

    // == != (binary)
    private Expression parseExprG() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprH();
        Token oper = scan.peek();
        while (acceptOpt(EQUAL_TO, NOT_EQUAL) != null) {
            expr = new BinaryExpr(new Operator(oper, 2), expr, parseExprH(), start.posn);
            oper = scan.peek();
        }
        return expr;
    }

    // <= < > >= (binary)
    private Expression parseExprH() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprI();
        Token oper = scan.peek();
        while (acceptOpt(LESS_EQUAL, LESS_THAN, GREATER_THAN, GREATER_EQUAL) != null) {
            expr = new BinaryExpr(new Operator(oper, 2), expr, parseExprI(), start.posn);
            oper = scan.peek();
        }
        return expr;
    }

    // + - (binary)
    private Expression parseExprI() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprJ();
        Token oper = scan.peek();
        while (acceptOpt(PLUS, MINUS) != null) {
            expr = new BinaryExpr(new Operator(oper, 2), expr, parseExprJ(), start.posn);
            oper = scan.peek();
        }
        return expr;
    }

    // * / (binary)
    private Expression parseExprJ() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprK();
        Token oper = scan.peek();
        while (acceptOpt(MULTIPLY, DIVIDE) != null) {
            expr = new BinaryExpr(new Operator(oper, 2), expr, parseExprK(), start.posn);
            oper = scan.peek();
        }
        return expr;
    }

    // - ! (unary)
    private Expression parseExprK() throws SyntaxException {
        Token oper = scan.peek();
        if (acceptOpt(MINUS, NOT) != null) {
            return new UnaryExpr(new Operator(oper, 1), parseExprK(), oper.posn);
        } else {
            return parseExprBase();
        }
    }

    @SuppressWarnings("incomplete-switch")
    private Expression parseExprBase() throws SyntaxException {
        Token first = scan.peek();
        Expression expr = null;
        Kind firstKind = acceptOpt(LPAREN, NEW, NUM, TRUE, FALSE, NULL);
        if (firstKind == null) {
            // Must be a Reference
            Reference ref = parseReference();
            if (acceptOpt(Kind.LBRACKET) != null) {
                // Array access
                expr = new IxExpr(ref, parseExpression(), first.posn);
                accept(Kind.RBRACKET);
            } else if (acceptOpt(Kind.LPAREN) != null) {
                // Function call
                expr = new CallExpr(ref, parseArgumentsForCall(), first.posn);
                accept(Kind.RPAREN);
            } else {
                // Just a reference
                expr = new RefExpr(ref, first.posn);
            }
        } else {
            // Not a reference
            switch (firstKind) {
                case LPAREN:
                    expr = parseExpression();
                    accept(RPAREN);
                    break;
                case NEW:
                    Token prev = scan.peek();
                    if (acceptOpt(Kind.INT) != null) {
                        // Must be an int array (only time new is used with int)
                        accept(Kind.LBRACKET);
                        expr = new NewArrayExpr(new BaseType(TypeKind.INT, prev.posn),
                                parseExpression(), first.posn);
                        accept(Kind.RBRACKET);
                    } else {
                        accept(Kind.ID);
                        if (acceptOpt(Kind.LPAREN) != null) {
                            // Must be a class "constructor"
                            expr = new NewObjectExpr(new ClassType(prev.spelling, prev.posn),
                                    first.posn);
                            accept(Kind.RPAREN);
                        } else {
                            // Must be an array declaration
                            accept(Kind.LBRACKET);
                            expr = new NewArrayExpr(new ClassType(prev.spelling, prev.posn),
                                    parseExpression(), first.posn);
                            accept(Kind.RBRACKET);
                        }
                    }
                    break;
                case NUM:
                    expr = new LiteralExpr(new IntLiteral(first), first.posn);
                    break;
                case TRUE:
                    expr = new LiteralExpr(new BooleanLiteral(first), first.posn);
                    break;
                case FALSE:
                    expr = new LiteralExpr(new BooleanLiteral(first), first.posn);
                    break;
                case NULL:
                    expr = new NullExpr(first.posn);
            }
        }
        return expr;
    }

    /*----------------------------------------------------*
     * Private helper methods                             *
     *----------------------------------------------------*/

    // verify that current token in input matches expections error if not, and
    // advance
    private Kind accept(Kind... expectedTypes) throws SyntaxException {
        Token token = scan.peek();
        Kind kind = token.kind;
        if (Arrays.stream(expectedTypes).anyMatch(kind::equals)) {
            if (trace) pTrace(token);
            scan.pop();
            return kind;
        }
        throw parseError(String.format("Unexpected token %s at %s", kind, token.posn));
    }

    // advance only if the next token in input
    private Kind acceptOpt(Kind... expectedTypes) throws SyntaxException {
        Token token = scan.peek();
        Kind kind = token.kind;
        if (Arrays.stream(expectedTypes).anyMatch(kind::equals)) {
            if (trace) pTrace(token);
            scan.pop();
            return kind;
        }
        return null;
    }

    // show parse stack whenever terminal is accepted
    private void pTrace(Token token) {
        StackTraceElement[] stl = Thread.currentThread().getStackTrace();
        boolean printing = false;
        for (int i = stl.length - 1; i > 0; i--) {
            if (stl[i].toString().contains("parseProgram")) printing = true;
            if (printing && stl[i].toString().contains("parse")) System.out.println(stl[i]);
        }
        System.out.println(String.format("Accepting %s (\"%s\") at %s", token.kind, token.spelling,
                token.posn));
        System.out.println();
    }

    // report parse error and unwind call stack to start of parse
    private SyntaxException parseError(String e) {
        reporter.reportError("Parse error: " + e);
        return new SyntaxException();
    }
}
