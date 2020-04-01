package me.hammerle.snuviscript.code;

@FunctionalInterface
public interface ExceptionBiFunction<T, U, R> {
    public R apply(T t, U u) throws Exception;
}
