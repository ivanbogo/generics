package test.me;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface Container<T> {
    <S extends T> List<AtomicReference<S>> get(Typed<S> typed);
}
