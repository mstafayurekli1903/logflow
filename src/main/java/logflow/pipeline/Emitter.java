package logflow.pipeline;

public interface Emitter<T> {
    void emit(T item);
}
