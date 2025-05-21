package UI.components.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class ObservableConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
    private final CopyOnWriteArrayList<BiConsumer<K, V>> removeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BiConsumer<K, V>> addListeners = new CopyOnWriteArrayList<>();

    @Override
    public V remove(Object key) {
        V value = super.remove(key);
        if (value != null) {
            notifyRemoveListeners((K) key, value);
        }
        return value;
    }

    @Override
    public V put(K key, V value) {
        V oldValue = super.put(key, value);
        if (oldValue == null) {
            notifyAddListeners(key, value);
        }
        return oldValue;
    }

    public void addRemoveListener(BiConsumer<K, V> listener) {
        removeListeners.add(listener);
    }

    public void addAddListener(BiConsumer<K, V> listener) {
        addListeners.add(listener);
    }

    private void notifyRemoveListeners(K key, V value) {
        for (BiConsumer<K, V> listener : removeListeners) {
            listener.accept(key, value);
        }
    }

    private void notifyAddListeners(K key, V value) {
        for (BiConsumer<K, V> listener : addListeners) {
            listener.accept(key, value);
        }
    }
}
