package UI.scripts;

public class MutableValue<T> {
    private T value;

    public MutableValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}

