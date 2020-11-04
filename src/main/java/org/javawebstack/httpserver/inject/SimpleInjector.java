package org.javawebstack.httpserver.inject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SimpleInjector implements Injector {

    private final Map<Class<?>, Object> instances = new HashMap<>();

    public <T> T inject(T object) {
        inject(object.getClass(), object);
        return object;
    }

    private void inject(Class<?> clazz, Object object){
        if(!Object.class.equals(clazz.getSuperclass()))
            inject(clazz.getSuperclass(), object);
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
