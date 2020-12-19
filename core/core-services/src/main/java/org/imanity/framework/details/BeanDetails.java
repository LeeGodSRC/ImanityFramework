package org.imanity.framework.details;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface BeanDetails {

    boolean shouldInitialize() throws InvocationTargetException, IllegalAccessException;

    void call(Class<? extends Annotation> annotation) throws InvocationTargetException, IllegalAccessException;

    boolean isStage(GenericBeanDetails.ActivationStage stage);

    boolean isActivated();

    boolean isDestroyed();

    @Nullable
    String getTag(String key);

    boolean hasTag(String key);

    void addTag(String key, String value);

    void setName(String name);

    void setStage(GenericBeanDetails.ActivationStage stage);

    void setDisallowAnnotations(java.util.Map<Class<? extends Annotation>, String> disallowAnnotations);

    void setAnnotatedMethods(java.util.Map<Class<? extends Annotation>, java.util.Collection<Method>> annotatedMethods);

    void setInstance(Object instance);

    void setType(Class<?> type);

    void setTags(java.util.Map<String, String> tags);

    String getName();

    GenericBeanDetails.ActivationStage getStage();

    java.util.Map<Class<? extends Annotation>, String> getDisallowAnnotations();

    java.util.Map<Class<? extends Annotation>, java.util.Collection<Method>> getAnnotatedMethods();

    @Nullable
    Object getInstance();

    Class<?> getType();

    java.util.Map<String, String> getTags();

}
