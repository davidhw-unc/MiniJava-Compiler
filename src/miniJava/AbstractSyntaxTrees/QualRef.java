/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class QualRef extends Reference {
    public Reference prevRef;

    public QualRef(Reference ref, Identifier id, SourcePosition posn) {
        super(posn);
        this.prevRef = ref;
        this.setId(id);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitQualRef(this, o);
    }
}
