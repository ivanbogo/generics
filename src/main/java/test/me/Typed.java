package test.me;

public class Typed<T> {
    private final int ordinal;
    private final Class<T> clazz;

    public Typed(int ordinal, Class<T> clazz) {
        this.ordinal = ordinal;
        this.clazz = clazz;
    }

    public int ordinal() { return ordinal; }
    public Class<T> clazz() { return clazz; }
}
