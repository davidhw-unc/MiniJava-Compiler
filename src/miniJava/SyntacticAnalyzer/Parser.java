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

    private static final boolean trace = true;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scan = scanner;
        this.reporter = reporter;
    }

    private static class SyntaxException extends Exception {
        private static final long serialVersionUID = 1L;
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
                    new BaseType(TypeKind.VOID, prev.posn), id.spelling, id.posn));
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
                type = new ClassType(new Identifier(typeToken), typeToken.posn);
                break;
        }
        if (kind != BOOLEAN) {
            Token lbToken = scan.peek();
            if (acceptOpt(LBRACKET) != null) {
                accept(RBRACKET);
                type = new ArrayType(type, lbToken.posn);
            }
        }
        return type;
    }

    // Nullable (i.e. may consume no tokens and return an empty ExprList)
    private ExprList parseArgumentsForCall() throws SyntaxException {
        ExprList args = new ExprList();
        if (scan.peek().kind != RPAREN) {
            while (acceptOpt(COMMA) != null) {
                args.add(parseExpression());
            }
        }
        return args;
    }

    @SuppressWarnings("incomplete-switch")
    private Reference parseReference() throws SyntaxException {
        Reference ref = null;
        Token original = scan.peek();
        switch (accept(ID, THIS)) {
            case ID:
                ref = new IdRef(new Identifier(original), original.posn);
                break;
            case THIS:
                ref = new ThisRef(original.posn);
                break;
        }
        while (acceptOpt(DOT) != null) {
            Token prev = scan.peek();
            accept(ID);
            ref = new QualRef(ref, new Identifier(prev), original.posn);
        }
        return ref;
    }

    @SuppressWarnings("incomplete-switch")
    private Statement parseStatement() throws SyntaxException {
        Kind firstKind = acceptOpt(LBRACE, RETURN, IF, WHILE);
        if (firstKind == null) {
            switch (scan.peek().kind) {
                case INT:
                case BOOLEAN:
                    parseType();
                    accept(ID);
                    parseAssignment();
                    break;
                case THIS:
                    parseReference();
                    if (acceptOpt(LPAREN) != null) {
                        // Function call
                        if (acceptOpt(RPAREN) == null) {
                            parseArgumentsForCall();
                            accept(RPAREN);
                        }
                    } else {
                        // Array access w/ assignment
                        if (scan.peek().kind == LBRACKET) {
                            accept(LBRACKET);
                            parseExpression();
                            accept(RBRACKET);
                        }
                        parseAssignment();
                    }
                    break;
                case ID:
                    accept(ID);
                    boolean foundLBforReference = false, foundLBforType = false;
                    if (acceptOpt(LBRACKET) != null) { // Can either be a Type or a Reference 
                        if (acceptOpt(RBRACKET) != null) { // Must be a Type
                            foundLBforType = true;
                            accept(ID);
                        } else {
                            foundLBforReference = true;
                        }
                    }
                    if (foundLBforType || (!foundLBforReference && acceptOpt(ID) != null)) {
                        // Must have been a Type
                        parseAssignment();
                    } else {
                        if (!foundLBforReference && acceptOpt(DOT) != null) {
                            // Must still be in a reference
                            if (scan.peek().kind == THIS) {
                                // Special case: don't allow the next token to be THIS
                                throw parseError(String.format("Expected ID but found THIS at %s",
                                        scan.peek().posn));
                            }
                            parseReference();
                        }
                        // Must have reached the end of the reference
                        if (foundLBforReference || acceptOpt(LBRACKET) != null) {
                            // Array access w/ assignment
                            parseExpression();
                            accept(RBRACKET);
                            parseAssignment();
                        } else if (acceptOpt(LPAREN) != null) {
                            // Function call
                            if (acceptOpt(RPAREN) == null) {
                                parseArgumentsForCall();
                                accept(RPAREN);
                            }
                        } else {
                            // Regular reference w/ assignment
                            parseAssignment();
                        }
                    }
                    break;
            }
            accept(SEMICOLON);
        } else {
            switch (firstKind) {
                case LBRACE:
                    while (acceptOpt(RBRACE) == null) {
                        parseStatement();
                    }
                    break;
                case RETURN:
                    if (acceptOpt(SEMICOLON) == null) {
                        parseExpression();
                        accept(SEMICOLON);
                    }
                    break;
                case IF:
                    accept(LPAREN);
                    parseExpression();
                    accept(RPAREN);
                    parseStatement();
                    if (acceptOpt(ELSE) != null) {
                        parseStatement();
                    }
                    break;
                case WHILE:
                    accept(LPAREN);
                    parseExpression();
                    accept(RPAREN);
                    parseStatement();
                    break;
            }
        }
    }

    private void parseAssignment() throws SyntaxException {
        accept(ASSIGN);
        parseExpression();
    }

    @SuppressWarnings("incomplete-switch")
    private Expression parseExpressionOld() throws SyntaxException {
        Token firstToken = scan.peek();
        Expression expr = null;

        Kind firstKind = acceptOpt(NOT, MINUS, LPAREN, NEW, NUM, TRUE, FALSE);
        if (firstKind == null) {
            Reference ref = parseReference();
            if (acceptOpt(Kind.LBRACKET) != null) {
                // Array access
                expr = new IxExpr(ref, parseExpression(), firstToken.posn);
                accept(Kind.RBRACKET);
            } else if (acceptOpt(Kind.LPAREN) != null) {
                // Function call
                if (acceptOpt(Kind.RPAREN) == null) {
                    expr = new CallExpr(ref, parseArgumentsForCall(), firstToken.posn);
                    accept(Kind.RPAREN);
                }
            } else {
                // Just a reference
                expr = new RefExpr(ref, firstToken.posn);
            }
        } else {
            switch (firstKind) {
                case NOT:
                case MINUS:
                    parseExpression();
                    break;
                case LPAREN:
                    parseExpression();
                    accept(RPAREN);
                    break;
                case NEW:
                    if (acceptOpt(Kind.INT) != null) {
                        accept(Kind.LBRACKET);
                        parseExpression();
                        accept(Kind.RBRACKET);
                    } else {
                        accept(Kind.ID);
                        if (acceptOpt(Kind.LPAREN) != null) {
                            accept(Kind.RPAREN);
                        } else {
                            accept(Kind.LBRACKET);
                            parseExpression();
                            accept(Kind.RBRACKET);
                        }
                    }
                    break;
                case NUM:
                case TRUE:
                case FALSE:
                    break;
            }
        }
        // Handle binary operators
        Token oper = scan.peek();
        if (acceptOpt(Kind.GREATER_THAN, Kind.LESS_THAN, Kind.EQUAL_TO, Kind.LESS_EQUAL,
                Kind.GREATER_EQUAL, Kind.NOT_EQUAL, Kind.AND, Kind.OR, Kind.PLUS, Kind.MINUS,
                Kind.MULTIPLY, Kind.DIVIDE) != null) {
            parseExpression();
        }

        return null;
    }

    // ------------------
    // Expression parsing
    // ------------------

    // || (binary)
    private Expression parseExpression() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        if (acceptOpt(OR) != null) {
            expr = new BinaryExpr(new Operator(oper), expr, parseExprF(), start.posn);
        }
        return expr;
    }

    // && (binary)
    private Expression parseExprF() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        if (acceptOpt(AND) != null) {
            expr = new BinaryExpr(new Operator(oper), expr, parseExprF(), start.posn);
        }
        return expr;
    }

    // == != (binary)
    private Expression parseExprG() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        if (acceptOpt(EQUAL_TO, NOT_EQUAL) != null) {
            expr = new BinaryExpr(new Operator(oper), expr, parseExprF(), start.posn);
        }
        return expr;
    }

    // <= < > >= (binary)
    private Expression parseExprH() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        if (acceptOpt(LESS_EQUAL, LESS_THAN, GREATER_THAN, GREATER_EQUAL) != null) {
            expr = new BinaryExpr(new Operator(oper), expr, parseExprF(), start.posn);
        }
        return expr;
    }

    // + - (binary)
    private Expression parseExprI() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        if (acceptOpt(PLUS, MINUS) != null) {
            expr = new BinaryExpr(new Operator(oper), expr, parseExprF(), start.posn);
        }
        return expr;
    }

    // * / (binary)
    private Expression parseExprJ() throws SyntaxException {
        Token start = scan.peek();
        Expression expr = parseExprF();
        Token oper = scan.peek();
        if (acceptOpt(MULTIPLY, DIVIDE) != null) {
            expr = new BinaryExpr(new Operator(oper), expr, parseExprF(), start.posn);
        }
        return expr;
    }

    // - ! (unary)
    private Expression parseExprK() throws SyntaxException {
        Token oper = scan.peek();
        if (acceptOpt(MINUS, NOT) != null) {
            return new UnaryExpr(new Operator(oper), parseExprK(), oper.posn);
        } else {
            return parseExprBase();
        }
    }

    private Expression parseExprBase() throws SyntaxException {
        Token first = scan.peek();
        Expression expr = null;
        Kind firstKind = acceptOpt(LPAREN, NEW, NUM, TRUE, FALSE);
        if (firstKind == null) {
            // Must be a Reference
            Reference ref = parseReference();
            if (acceptOpt(Kind.LBRACKET) != null) {
                // Array access
                expr = new IxExpr(ref, parseExpression(), first.posn);
                accept(Kind.RBRACKET);
            } else if (acceptOpt(Kind.LPAREN) != null) {
                // Function call
                if (acceptOpt(Kind.RPAREN) == null) {
                    expr = new CallExpr(ref, parseArgumentsForCall(), first.posn);
                    accept(Kind.RPAREN);
                }
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
                            expr = new NewObjectExpr(new ClassType(new Identifier(prev), prev.posn),
                                    first.posn);
                            accept(Kind.RPAREN);
                        } else {
                            // Must be an array declaration
                            accept(Kind.LBRACKET);
                            expr = new NewArrayExpr(new ClassType(new Identifier(prev), prev.posn),
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
            }
        }

        // TODO this
        return null;
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
        if (expectedTypes.length == 1) {
            throw parseError(String.format("Expected %s but found %s at %s", expectedTypes[0],
                    kind.toString(), token.posn));
        }
        throw parseError(String.format("Expected one of %s but found %s at %s",
                Arrays.toString(expectedTypes), kind, token.posn));
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
