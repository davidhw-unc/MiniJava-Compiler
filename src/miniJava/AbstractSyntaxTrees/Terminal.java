/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.Token.Kind;

abstract public class Terminal extends AST {
    public Kind kind;
    public String spelling;

    public Terminal(Token t) {
        super(t.posn);
        spelling = t.spelling;
        kind = t.kind;
    }
}
