package net.thecodersbreakfast.annotationinjector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnnotationInjector {

    protected final Class<?> targetClass;
    private final String targetClassName;
    private Field annotationsMapAccessor;
    private Object annotationDataHolder;

    public AnnotationInjector(Class<?> targetClass) throws AnnotationInjectionException {
        if (targetClass == null) {
            throw new NullPointerException("The target class must not be null.");
        }
        this.targetClass = targetClass;
        this.targetClassName = targetClass.getCanonicalName();

        try {
            initAnnotationsMapAccessor();
        } catch (Exception e) {
            throw new AnnotationInjectionException("Could not initialize the Annotation injector for class " + targetClass.getCanonicalName(), e);
        }
    }

    public void injectAnnotations(Collection<Annotation> annotations) throws AnnotationInjectionException {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                injectAnnotation(annotation);
            }
        }
    }

    public void injectAnnotation(Annotation annotation) throws AnnotationInjectionException {
        if (annotation == null) {
            throw new NullPointerException("Cannot inject a null annotation.");
        }

        String annotationName = annotation.annotationType().getCanonicalName();

        if (annotationConflictsWithExistingAnnotation(annotation)) {
            throw new AnnotationInjectionException("Annotation " + annotationName +
                    " cannot be injected in class " + targetClassName +
                    " because it already exists with different parameter values.");
        }

        if (annotationAlreadyExists(annotation)) {
            return;
        }

        try {
            Map<Class<? extends Annotation>, Annotation> annotationsMap = getAnnotationsMap();
            annotationsMap.put(annotation.annotationType(), annotation);
            saveAnnotationsMap(annotationsMap);
        } catch (IllegalAccessException e) {
            throw new AnnotationInjectionException("Could not inject annotation " + annotationName + " in class " + targetClassName, e);
        }
    }

    private void initAnnotationsMapAccessor()
            throws NoSuchFieldException,
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException {
        targetClass.getAnnotations();
        if (System.getProperty("java.version").startsWith("1.8")) {
            Method annotationDataMethod = Class.class.getDeclaredMethod("annotationData");
            annotationDataMethod.setAccessible(true);
            annotationDataHolder = annotationDataMethod.invoke(targetClass);
            annotationsMapAccessor = annotationDataHolder.getClass().getDeclaredField("annotations");
        } else {
            annotationsMapAccessor = Class.class.getDeclaredField("annotations");
            annotationDataHolder = targetClass;
        }
        annotationsMapAccessor.setAccessible(true);
    }

    private Map<Class<? extends Annotation>, Annotation> getAnnotationsMap() throws IllegalAccessException {
        @SuppressWarnings("unchecked")
        Map<Class<? extends Annotation>, Annotation> annotationMap = (Map<Class<? extends Annotation>, Annotation>) annotationsMapAccessor.get(annotationDataHolder);
        if (annotationMap == null || annotationMap.isEmpty()) {
            annotationMap = new HashMap<>();
        }
        return annotationMap;
    }

    private void saveAnnotationsMap(Map<Class<? extends Annotation>, Annotation> annotationsMap) throws IllegalAccessException {
        annotationsMapAccessor.set(annotationDataHolder, annotationsMap);
    }

    private boolean annotationConflictsWithExistingAnnotation(Annotation annotation) {
        Annotation existingAnnotation = targetClass.getAnnotation(annotation.annotationType());
        return existingAnnotation != null && !existingAnnotation.equals(annotation);
    }

    private boolean annotationAlreadyExists(Annotation annotation) {
        Annotation existingAnnotation = targetClass.getAnnotation(annotation.annotationType());
        return existingAnnotation != null;
    }

}