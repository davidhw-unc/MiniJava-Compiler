package miniJava.SyntacticAnalyzer;

import static miniJava.SyntacticAnalyzer.Token.Kind.*;

import java.util.Arrays;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token.Kind;

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

    public void parse() {
        try {
            parseProgram();
        } catch (SyntaxException e) {
        }
    }

    /*----------------------------------------------------*
     * Parsing methods                                    *
     *----------------------------------------------------*/

    private void parseProgram() throws SyntaxException {
        while (acceptOpt(EOT) == null) {
            parseClassDec();
        }
    }

    private void parseClassDec() throws SyntaxException {
        accept(CLASS);
        accept(ID);
        accept(LBRACE);
        while (acceptOpt(RBRACE) == null) {
            parseMember();
        }
    }

    private void parseMember() throws SyntaxException {
        acceptOpt(PUBLIC, PRIVATE);
        acceptOpt(STATIC);
        if (acceptOpt(VOID) != null) {
            accept(ID);
            parseMethodDec();
        } else {
            parseType();
            accept(ID);
            if (acceptOpt(SEMICOLON) == null) {
                parseMethodDec();
            }
        }
    }

    private void parseMethodDec() throws SyntaxException {
        accept(LPAREN);
        if (acceptOpt(RPAREN) == null) {
            parseParameters();
            accept(RPAREN);
        }
        accept(LBRACE);
        while (acceptOpt(RBRACE) == null) {
            parseStatement();
        }
    }

    private void parseType() throws SyntaxException {
        Kind kind = accept(INT, BOOLEAN, ID);
        if (kind != BOOLEAN) {
            if (acceptOpt(LBRACKET) != null) accept(RBRACKET);
        }
    }

    private void parseParameters() throws SyntaxException {
        parseType();
        accept(ID);
        while (acceptOpt(COMMA) != null) {
            parseType();
            accept(ID);
        }
    }

    private void parseArguments() throws SyntaxException {
        parseExpression();
        while (acceptOpt(COMMA) != null) {
            parseExpression();
        }
    }

    private void parseReference() throws SyntaxException {
        accept(ID, THIS);
        while (acceptOpt(DOT) != null) {
            accept(ID);
        }
    }

    @SuppressWarnings("incomplete-switch")
    private void parseStatement() throws SyntaxException {
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
                    if (scan.peek().kind == LBRACKET) {
                        accept(LBRACKET);
                        parseExpression();
                        accept(RBRACKET);
                    }
                    parseAssignment();
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
                                throw parseError(
                                        String.format("Expected ID but found THIS at <%d:%d>",
                                                scan.peek().line, scan.peek().startColumn));
                            }
                            parseReference();
                        }
                        // Must have reached the end of the reference
                        if (foundLBforReference || acceptOpt(LBRACKET) != null) {
                            // Array indexing followed by assignment
                            parseExpression();
                            accept(RBRACKET);
                            parseAssignment();
                        } else if (acceptOpt(LPAREN) != null) {
                            // Function call
                            if (acceptOpt(RPAREN) == null) {
                                parseArguments();
                                accept(RPAREN);
                            }
                        } else {
                            // Regular reference followed by assignment
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
    private void parseExpression() throws SyntaxException {
        Kind firstKind = acceptOpt(NOT, MINUS, LPAREN, NEW, NUM, TRUE, FALSE);
        if (firstKind == null) {
            parseReference();
            if (acceptOpt(Kind.LBRACKET) != null) {
                parseExpression();
                accept(Kind.RBRACKET);
            } else if (acceptOpt(Kind.LPAREN) != null) {
                if (acceptOpt(Kind.RPAREN) == null) {
                    parseArguments();
                    accept(Kind.RPAREN);
                }
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
        if (acceptOpt(Kind.GREATER_THAN, Kind.LESS_THAN, Kind.EQUAL_TO, Kind.LESS_EQUAL,
                Kind.GREATER_EQUAL, Kind.NOT_EQUAL, Kind.AND, Kind.OR, Kind.PLUS, Kind.MINUS,
                Kind.MULTIPLY, Kind.DIVIDE) != null) {
            parseExpression();
        }
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
            throw parseError(String.format("Expected %s but found %s at <%d:%d>", expectedTypes[0],
                    kind.toString(), token.line, token.startColumn));
        }
        throw parseError(String.format("Expected one of %s but found %s at <%d:%d>",
                Arrays.toString(expectedTypes), kind, token.line, token.startColumn));
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
        System.out.println(String.format("Accepting %s (\"%s\") at <%d:%d>", token.kind,
                token.contents, token.line, token.startColumn));
        System.out.println();
    }

    // report parse error and unwind call stack to start of parse
    private SyntaxException parseError(String e) {
        reporter.reportError("Parse error: " + e);
        return new SyntaxException();
    }
}
