package logflow.pipeline;

import java.util.ArrayList;
import java.util.List;

public class CollectingEmitter<T> implements Emitter<T> {
    private final List<T> emitted = new ArrayList<>();

    @Override
    public void emit(T item) {
        emitted.add(item);
    }

    public List<T> getEmitted() {
        return emitted;
    }
}
