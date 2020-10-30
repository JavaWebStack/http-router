package org.javawebstack.httpserver.inject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SimpleInjector implements Injector {

    private final Map<Class<?>, Object> instances = new HashMap<>();

    public <T> T inject(T object) {
        for(Field field : object.getClass().getDeclaredFields()){
            if(field.getDeclaredAnnotationsByType(Inject.class).length != 0){
                if(instances.containsKey(field.getType())){
                    try {
                        field.setAccessible(true);
                        field.set(object, getInstance(field.getType()));
                    } catch (IllegalAccessException ignored) {}
                }
            }
        }
        return object;
    }

    public <T> T getInstance(Class<T> clazz) {
        return (T) instances.get(clazz);
    }

    public <T> void setInstance(Class<T> clazz, T instance) {
        setInstanceUnsafe(clazz, instance);
    }

    public void setInstanceUnsafe(Class<?> clazz, Object instance){
        instances.put(clazz, instance);
    }

}
