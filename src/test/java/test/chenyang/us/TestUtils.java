package test.chenyang.us;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

public class TestUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private TestUtils() {
    }

    private static void assertUtilityClassClassWellDefined(final Class<?> clazz) {
        Preconditions.checkState(Modifier.isFinal(clazz.getModifiers()), "class must be final");
        Preconditions.checkArgument(clazz.getDeclaredConstructors().length == 1, "There must be only one constructor");

    }

    private static void assertUtilityClassConstructorWellDefined(final Constructor<?> constructor)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Preconditions.checkState(!constructor.isAccessible(), "Constructor is not private");
        Preconditions.checkState(Modifier.isPrivate(constructor.getModifiers()), "Constructor is not private");

        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    private static void assertUtilityClassMethodsWellDefined(final Class<?> clazz) {
        for (final Method method : clazz.getMethods()) {
            Preconditions.checkState(
                    Modifier.isStatic(method.getModifiers()) || !method.getDeclaringClass().equals(clazz),
                    "there exists a non-static method:" + method);
        }
    }

    public static void assertUtilityClassWellDefined(final Class<?> clazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        assertUtilityClassClassWellDefined(clazz);

        final Constructor<?> constructor = clazz.getDeclaredConstructor();
        assertUtilityClassConstructorWellDefined(constructor);
        assertUtilityClassMethodsWellDefined(clazz);
    }

    public static String getJson(Class<?> clazz, String jsonFilePath) throws Exception {
        String result = IOUtils.toString(clazz.getResourceAsStream(jsonFilePath), Charset.defaultCharset());

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <F> F getMemberField(Object obj, String fieldName) {
        try {
            final Field f = obj.getClass().getDeclaredField(fieldName);

            AccessController.doPrivileged(new PrivilegedAction<Field>() {
                @Override
                public Field run() {
                    f.setAccessible(true);
                    return null;
                }
            });

            return (F) f.get(obj);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            return null;
        }
    }

    public static Properties getProperties(Class<?> clazz, String propertiesFilePath) {
        Properties properties = new Properties();

        try (InputStream is = clazz.getResourceAsStream(propertiesFilePath)) {
            properties.load(is);
        } catch (IOException ex) {
        }

        return properties;
    }

    public static void verify(Object obj) throws IOException {
        verify(obj, obj.getClass());
    }

    public static <T> void verify(Object obj, Class<T> clazz) throws IOException {
        String read = MAPPER.writeValueAsString(obj);

        T another = MAPPER.readValue(read, clazz);

        assert Objects.equals(obj, another);
    }
}
