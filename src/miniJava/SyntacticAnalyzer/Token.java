package miniJava.SyntacticAnalyzer;

public class Token {
    public enum Kind {
        ID, NUM,

        CLASS, VOID, PUBLIC, PRIVATE, STATIC, INT, BOOLEAN, THIS, RETURN, IF, ELSE, WHILE, TRUE,
        FALSE, NEW, NULL, FOR,

        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE, SEMICOLON,

        LESS_THAN, GREATER_THAN, EQUAL_TO, LESS_EQUAL, GREATER_EQUAL, ASSIGN, NOT, NOT_EQUAL, AND,
        OR, PLUS, MINUS, MULTIPLY, DIVIDE, COMMA, DOT, MODULUS, Q_MARK, COLON,

        EOT, ERROR
    }

    public final Kind kind;
    public final String spelling;
    public final SourcePosition posn;

    public Token(Kind kind, String contents, long line, int startColumn) {
        this.kind = kind;
        this.spelling = contents;
        posn = new SourcePosition(line, startColumn);
    }

    @Override
    public String toString() {
        return String.format("%-13s %8s %s", kind.toString(), posn, spelling);
    }
}
