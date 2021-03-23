package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token.Kind;

public class Scanner implements Iterable<Token> {
    private InputStream iStream;
    private ErrorReporter reporter;

    private char curChar;
    private boolean eot = false;
    private Token nextToken = null;
    private long line = 1;
    private int column = 0;

    public Scanner(InputStream iStream, ErrorReporter reporter) {
        this.iStream = iStream;
        this.reporter = reporter;
        readChar();
        pop();
    }

    public Token peek() {
        return nextToken;
    }

    public void pop() {
        while (!eot && Character.isWhitespace(curChar)) {
            skipIt();
        }

        StringBuilder curContents = new StringBuilder();
        Kind kind;

        if (eot) {
            kind = Kind.EOT;
        } else if (Character.isAlphabetic(curChar)) {
            do {
                takeIt(curContents);
            } while (Character.isAlphabetic(curChar) || Character.isDigit(curChar)
                    || curChar == '_');
            String contents = curContents.toString();
            try {
                if (contents.equals(contents.toLowerCase())) {
                    kind = Kind.valueOf(curContents.toString().toUpperCase());
                } else {
                    kind = Kind.ID;
                }
            } catch (IllegalArgumentException e) {
                kind = Kind.ID;
            }
        } else if (Character.isDigit(curChar)) {
            kind = Kind.NUM;
            do {
                takeIt(curContents);
            } while (Character.isDigit(curChar));
        } else {
            switch (curChar) {

                case '/': // Comment handling (as well as division token)
                    takeIt(curContents);
                    switch (curChar) {
                        case '/':
                            // We're in a line comment, so skip to eol and call again
                            while (curChar != '\n' && !eot) {
                                skipIt();
                            }
                            skipIt();
                            pop();
                            return;
                        case '*':
                            // We're in a block comment, so skip to end of block and call again
                            skipIt();
                            boolean possEnd = false;
                            long commentStartRow = line;
                            int commentStartCol = column;
                            while ((!possEnd || curChar != '/') && !eot) {
                                possEnd = curChar == '*';
                                skipIt();
                            }
                            if (eot) {
                                scanError(String.format(
                                        "Unclosed block comment starting at <%d:%d>",
                                        commentStartRow, commentStartCol));
                                kind = Kind.ERROR;
                                curContents.setLength(0);
                            } else {
                                skipIt();
                                pop();
                                return;
                            }
                            break;
                        default:
                            // It's just a normal division sign
                            kind = Kind.DIVIDE;
                            break;
                    }
                    break;

                // @formatter:off
                case '(':   kind = Kind.LPAREN;       takeIt(curContents);    break;
                case ')':   kind = Kind.RPAREN;       takeIt(curContents);    break;
                case '[':   kind = Kind.LBRACKET;     takeIt(curContents);    break;
                case ']':   kind = Kind.RBRACKET;     takeIt(curContents);    break;
                case '{':   kind = Kind.LBRACE;       takeIt(curContents);    break;
                case '}':   kind = Kind.RBRACE;       takeIt(curContents);    break;
                case ';':   kind = Kind.SEMICOLON;    takeIt(curContents);    break;
                case '+':   kind = Kind.PLUS;         takeIt(curContents);    break;
                case '-':   kind = Kind.MINUS;        takeIt(curContents);    break;
                case '*':   kind = Kind.MULTIPLY;     takeIt(curContents);    break;
                case ',':   kind = Kind.COMMA;        takeIt(curContents);    break;
                case '.':   kind = Kind.DOT;          takeIt(curContents);    break;
                // @formatter:on

                case '<':
                    takeIt(curContents);
                    if (curChar == '=') {
                        kind = Kind.LESS_EQUAL;
                        takeIt(curContents);
                    } else {
                        kind = Kind.LESS_THAN;
                    }
                    break;
                case '>':
                    takeIt(curContents);
                    if (curChar == '=') {
                        kind = Kind.GREATER_EQUAL;
                        takeIt(curContents);
                    } else {
                        kind = Kind.GREATER_THAN;
                    }
                    break;
                case '=':
                    takeIt(curContents);
                    if (curChar == '=') {
                        kind = Kind.EQUAL_TO;
                        takeIt(curContents);
                    } else {
                        kind = Kind.ASSIGN;
                    }
                    break;
                case '!':
                    takeIt(curContents);
                    if (curChar == '=') {
                        kind = Kind.EQUAL_TO;
                        takeIt(curContents);
                    } else {
                        kind = Kind.NOT;
                    }
                    break;
                case '&':
                    takeIt(curContents);
                    if (curChar == '&') {
                        kind = Kind.AND;
                        takeIt(curContents);
                    } else {
                        kind = Kind.ERROR;
                        scanError(String.format("Second '&' expected at <%d:%d>", line, column));
                    }
                    break;
                case '|':
                    takeIt(curContents);
                    if (curChar == '|') {
                        kind = Kind.OR;
                        takeIt(curContents);
                    } else {
                        kind = Kind.ERROR;
                        scanError(String.format("Second '|' expected at <%d:%d>", line, column));
                    }
                    break;
                default:
                    takeIt(curContents);
                    scanError(String.format("Unknown symbol \"%s\" at <%d:%d>", curContents, line,
                            column));
                    kind = Kind.ERROR;
                    break;
            }
        }

        nextToken = new Token(kind, curContents.toString(), line, column - curContents.length());
    }

    @Override
    public Iterator<Token> iterator() {
        return new Iterator<Token>() {
            @Override
            public boolean hasNext() {
                return peek().kind != Kind.EOT;
            }

            @Override
            public Token next() {
                Token next = peek();
                pop();
                return next;
            }
        };
    }

    private void takeIt(StringBuilder curContents) {
        curContents.append(curChar);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private void nextChar() {
        if (!eot) readChar();
    }

    private void scanError(String m) {
        reporter.reportError("Scan error: " + m);
    }

    private void readChar() {
        try {
            int c = iStream.read();
            curChar = (char) c;
            if (c == -1) {
                eot = true;
            } else {
                if (curChar == '\n') {
                    ++line;
                    column = 0;
                } else {
                    ++column;
                }
            }
        } catch (IOException e) {
            scanError(String.format(
                    "I/O Exception (%d complete lines plus %d characters successfully read)",
                    line - 1, column - 1));
            eot = true;
        }
    }
}