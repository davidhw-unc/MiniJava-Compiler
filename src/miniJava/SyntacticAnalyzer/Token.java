package miniJava.SyntacticAnalyzer;

public class Token {
    public enum Kind {
        ID, NUM,

        CLASS, VOID, PUBLIC, PRIVATE, INT, BOOLEAN, THIS, RETURN, IF, ELSE, WHILE, NEW, TRUE, FALSE,
        STATIC,

        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE, SEMICOLON,

        LESS_THAN, GREATER_THAN, EQUAL_TO, LESS_EQUAL, GREATER_EQUAL, ASSIGN, NOT, NOT_EQUAL,
        AND, OR, PLUS, MINUS, MULTIPLY, DIVIDE, COMMA, DOT,

        EOT, ERROR
    }

    public final Kind kind;
    public final String contents;
    public final long line;
    public final int startColumn;

    public Token(Kind kind, String contents, long line, int startColumn) {
        this.kind = kind;
        this.contents = contents;
        this.line = line;
        this.startColumn = startColumn;
    }

    @Override
    public String toString() {
        return String.format("%-13s%5d%4d %s", kind.toString(), line, startColumn, contents);
    }
}
