package helpers.emulator.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CircularBuffer<T> {
    private final Object[] buffer;
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();

    public CircularBuffer(int capacity) {
        buffer = new Object[capacity];
    }

    public void add(T element) {
        lock.lock();
        try {
            int index = writeIndex.getAndIncrement() % buffer.length;
            buffer[index] = element;
        } finally {
            lock.unlock();
        }
    }

    public T addAndGetReplaced(T element) {
        lock.lock();
        try {
            int index = writeIndex.getAndIncrement() % buffer.length;
            T replaced = (T) buffer[index]; // Get the element being replaced
            buffer[index] = element; // Replace with the new element
            return replaced; // Return the replaced element for cleanup
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T getLatest() {
        lock.lock();
        try {
            int index = (writeIndex.get() - 1 + buffer.length) % buffer.length;
            return (T) buffer[index];
        } finally {
            lock.unlock();
        }
    }
}