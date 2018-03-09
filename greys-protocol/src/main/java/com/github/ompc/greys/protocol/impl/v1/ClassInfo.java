package com.github.ompc.greys.protocol.impl.v1;

import com.github.ompc.greys.protocol.Gp;
import com.github.ompc.greys.protocol.GpType;
import lombok.Data;

import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

import static com.github.ompc.greys.protocol.GpConstants.*;
import static com.github.ompc.greys.protocol.util.GpStringUtils.getGaClassName;
import static com.github.ompc.greys.protocol.util.GpStringUtils.getGaClassNames;

@Data
@Gp(GpType.CLASS_INFO)
public class ClassInfo {

    private final String name;
    private final String codeSource;
    private final int classType;
    private final int modifier;
    private final List<String> annotationTypes = new ArrayList<String>();
    private final List<String> interfaceTypes = new ArrayList<String>();
    private final List<String> familyTypes = new ArrayList<String>();
    private final List<String> classLoaders = new ArrayList<String>();

    public ClassInfo(final Class<?> clazz) {
        this.name = getGaClassName(clazz);
        this.codeSource = getCodeSource(clazz);
        this.classType = getClassType(clazz);
        this.modifier = clazz.getModifiers();
        this.annotationTypes.addAll(getGaClassNames(clazz.getDeclaredAnnotations()));
        this.interfaceTypes.addAll(getGaClassNames(clazz.getInterfaces()));
        this.familyTypes.addAll(getFamilyTypes(clazz.getSuperclass()));
        this.classLoaders.addAll(getClassLoaders(clazz.getClassLoader()));
    }

    private String getCodeSource(final Class<?> clazz) {
        final CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        return null != cs
                && null != cs.getLocation()
                && null != cs.getLocation().getFile()
                ? cs.getLocation().getFile()
                : EMPTY_STRING;
    }

    private int getClassType(final Class<?> clazz) {
        return markClassType(clazz.isAnnotation(), CLASS_TYPE_ANNOTATION)
                | markClassType(clazz.isInterface(), CLASS_TYPE_INTERFACE)
                | markClassType(clazz.isEnum(), CLASS_TYPE_ENUM)
                | markClassType(clazz.isAnonymousClass(), CLASS_TYPE_ANONYMOUS)
                | markClassType(clazz.isArray(), CLASS_TYPE_ARRAY)
                | markClassType(clazz.isLocalClass(), CLASS_TYPE_LOCAL)
                | markClassType(clazz.isMemberClass(), CLASS_TYPE_MEMBER)
                | markClassType(clazz.isPrimitive(), CLASS_TYPE_PRIMITIVE)
                | markClassType(clazz.isSynthetic(), CLASS_TYPE_SYNTHETIC);
    }

    private List<String> getFamilyTypes(final Class<?> superClass) {
        final List<String> familyTypes = new ArrayList<String>();
        if (null != superClass) {
            familyTypes.add(getGaClassName(superClass));
            familyTypes.addAll(getFamilyTypes(superClass.getSuperclass()));
        }
        return familyTypes;
    }

    private List<String> getClassLoaders(final ClassLoader loader) {
        final List<String> classLoaders = new ArrayList<String>();
        if (null != loader) {
            classLoaders.add(getGaClassName(loader.getClass()));
            classLoaders.addAll(getClassLoaders(loader.getParent()));
        }
        return classLoaders;
    }

}
