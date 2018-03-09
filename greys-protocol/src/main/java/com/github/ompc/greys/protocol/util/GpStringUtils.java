package com.github.ompc.greys.protocol.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GpStringUtils {

    public static final String getGaClassName(final Class<?> clazz) {
        return clazz.isArray()
                ? clazz.getCanonicalName()
                : clazz.getName();
    }

    public static final List<String> getGaClassNames(final Annotation[] annotationArray) {
        return getGaClassNames(getAnnotationTypeArray(annotationArray));
    }

    private static Class[] getAnnotationTypeArray(final Annotation[] annotationArray) {
        final Collection<Class> annotationTypes = new ArrayList<Class>();
        if( null != annotationArray ) {
            for (final Annotation annotation : annotationArray) {
                if (annotation.getClass().isAnnotation()) {
                    annotationTypes.add(annotation.getClass());
                }
                for (final Class annotationInterfaceClass : annotation.getClass().getInterfaces()) {
                    if (annotationInterfaceClass.isAnnotation()) {
                        annotationTypes.add(annotationInterfaceClass);
                    }
                }
            }
        }
        return annotationTypes.toArray(new Class[0]);
    }

    public static final List<String> getGaClassNames(final Class<?>[] classArray) {
        final List<String> gaClassNames = new ArrayList<String>();
        if (null != classArray) {
            for (final Class<?> clazz : classArray) {
                gaClassNames.add(getGaClassName(clazz));
            }
        }
        return gaClassNames;
    }

}
