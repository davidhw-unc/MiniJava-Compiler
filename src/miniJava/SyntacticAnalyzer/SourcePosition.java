package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    public final long line;
    public final int startColumn;

    public SourcePosition(long line, int startColumn) {
        this.line = line;
        this.startColumn = startColumn;
    }

    @Override
    public String toString() {
        return String.format("<%d:%d>", line, startColumn);
    }
}
