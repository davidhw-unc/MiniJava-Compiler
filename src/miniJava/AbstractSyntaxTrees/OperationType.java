package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token.Kind;

public class OperationType extends TypeDenoter {
    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    private static final TriFunction<TypeDenoter, TypeDenoter, TypeDenoter, TypeDenoter> genericBinary = (
            a, b, __) -> getTypeFromEq(a, b), genericUnary = (a, __, ___) -> a;

    private static final OperationType orOper = new OperationType(2, genericBinary);
    private static final OperationType andOper = new OperationType(2, genericBinary);
    private static final OperationType equalToOper = new OperationType(2, genericBinary);
    private static final OperationType notEqualOper = new OperationType(2, genericBinary);
    private static final OperationType lessEqualOper = new OperationType(2, genericBinary);
    private static final OperationType lessThanOper = new OperationType(2, genericBinary);
    private static final OperationType greaterThanOper = new OperationType(2, genericBinary);
    private static final OperationType greaterEqualOper = new OperationType(2, genericBinary);
    private static final OperationType plusOper = new OperationType(2, genericBinary);
    private static final OperationType minusOper = new OperationType(2, genericBinary);
    private static final OperationType multiplyOper = new OperationType(2, genericBinary);
    private static final OperationType divideOper = new OperationType(2, genericBinary);
    private static final OperationType negativeOper = new OperationType(1, genericUnary);
    private static final OperationType notOper = new OperationType(1, genericUnary);

    public static OperationType getType(Kind kind, int operandCount) {
        switch (operandCount) {
            case 1:
                switch (kind) {
                    case MINUS:
                        return negativeOper;
                    case NOT:
                        return notOper;
                    default:
                        throw new IllegalArgumentException(
                                "'-' and '!' are currently the only supported unary operators");
                }
            case 2:
                switch (kind) {
                    case OR:
                        return orOper;
                    case AND:
                        return andOper;
                    case EQUAL_TO:
                        return equalToOper;
                    case NOT_EQUAL:
                        return notEqualOper;
                    case LESS_EQUAL:
                        return lessEqualOper;
                    case LESS_THAN:
                        return lessThanOper;
                    case GREATER_THAN:
                        return greaterThanOper;
                    case GREATER_EQUAL:
                        return greaterEqualOper;
                    case PLUS:
                        return plusOper;
                    case MINUS:
                        return minusOper;
                    case MULTIPLY:
                        return multiplyOper;
                    case DIVIDE:
                        return divideOper;
                    case NOT:
                        throw new IllegalArgumentException("'!' is not a binary operator");
                    default:
                        throw new IllegalArgumentException(
                                "Invalid token kind: " + kind + " is not a binary operator");
                }
            default:
                throw new IllegalArgumentException(
                        "Currently only 1 and 2 argument operations are supported");
        }
    }

    private int operandCount;
    private TriFunction<TypeDenoter, TypeDenoter, TypeDenoter, TypeDenoter> f;

    private OperationType(int operandCount,
            TriFunction<TypeDenoter, TypeDenoter, TypeDenoter, TypeDenoter> f) {
        super(TypeKind.OPERATION, new SourcePosition(0, 0));
        this.operandCount = operandCount;
        this.f = f;
    }

    @Override
    public TypeDenoter getAndCheckType(TypeDenoter... types) {
        validateTypeCount(operandCount, types);
        // Building in room for a 3-operand ternary operator
        switch (operandCount) {
            case 1:
                return f.apply(types[0], null, null);
            case 2:
                return f.apply(types[0], types[1], null);
        }
        throw new IllegalStateException("This shouldn't be able to happen...");
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean eq(TypeDenoter other) {
        // TODO Auto-generated method stub
        return false;
    }
}
