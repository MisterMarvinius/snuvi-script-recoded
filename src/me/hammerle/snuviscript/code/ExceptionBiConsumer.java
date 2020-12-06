package me.hammerle.snuviscript.code;

@FunctionalInterface
public interface ExceptionBiConsumer<T, U> {
    public void apply(T t, U u) throws Exception;
}
