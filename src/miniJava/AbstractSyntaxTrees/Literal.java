package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public abstract class Literal extends Terminal implements Typed {
    private TypeDenoter type = null;

    public Literal(Token t, TypeDenoter type) {
        super(t);
        this.type = type;
    }

    @Override
    public TypeDenoter getType() {
        return type;
    }

    @Override
    public void setType(TypeDenoter type) {
        throw new UnsupportedOperationException("Cannot change the type of a literal");
    }
}
