package helpers.scripts.utils;

public class ByteArrayClassLoader extends ClassLoader {
    private final Script script;

    public ByteArrayClassLoader(ClassLoader parent, Script script) {
        super(parent);
        this.script = script;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        byte[] classData = script.getFile(path);
        if (classData == null) {
            throw new ClassNotFoundException("Class data not found for: " + name);
        }
        return defineClass(name, classData, 0, classData.length);
    }
}