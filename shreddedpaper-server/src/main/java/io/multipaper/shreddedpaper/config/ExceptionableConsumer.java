package io.multipaper.shreddedpaper.config;

@FunctionalInterface
public interface ExceptionableConsumer<T> {

    void accept(T t) throws Exception;

}
