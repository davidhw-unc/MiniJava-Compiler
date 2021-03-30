package miniJava.AbstractSyntaxTrees;

interface Typed {
    TypeDenoter getType();

    void setType(TypeDenoter type);
    //TypeDenoter getAndCheckType(TypeDenoter... types);

    /*
    static void validateTypeCount(int expectedCount, TypeDenoter[] types) {
        if (types.length != expectedCount) {
            throw new IllegalArgumentException(
                    "Invalid number of types passed for type validation");
        }
    }
    */
}
