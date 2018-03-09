package com.github.ompc.greys.protocol.v1;

import com.github.ompc.greys.protocol.GpConstants;
import com.github.ompc.greys.protocol.GpType;
import com.github.ompc.greys.protocol.GreysProtocol;
import com.github.ompc.greys.protocol.impl.v1.ClassInfo;
import com.github.ompc.greys.protocol.impl.v1.Thanks;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static com.github.ompc.greys.protocol.GpConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class ClassInfoTestCase {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new ClassInfoMatcher(java.lang.String.class)},
                {new ClassInfoMatcher(javax.swing.JFrame.class)},
                {new ClassInfoMatcher(java.net.SocketImpl.class)},
                {new ClassInfoMatcher(int.class)},
                {new ClassInfoMatcher(int[].class)},
                {new ClassInfoMatcher(int[][].class)},
                {new ClassInfoMatcher(Void.class)},
                {new ClassInfoMatcher(Thanks.Collaborator.class)},
        });
    }

    private final ClassInfoMatcher matcher;

    public ClassInfoTestCase(ClassInfoMatcher matcher) {
        this.matcher = matcher;
    }

    @Test
    public void test_for_serializeGpClassInfo() {
        final GreysProtocol<ClassInfo> gpClassInfo = new GreysProtocol<ClassInfo>(
                GpConstants.GP_VERSION_1_0_0,
                GpType.CLASS_INFO,
                new ClassInfo(matcher.getTargetClass())
        );
        Assert.assertThat(gpClassInfo, matcher);
    }


    static class ClassInfoMatcher extends BaseMatcher<GreysProtocol<ClassInfo>> {

        private final Class<?> clazz;

        ClassInfoMatcher(final Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("matching:" + getClassName());
        }

        public Class<?> getTargetClass() {
            return clazz;
        }

        private String getClassName() {
            return clazz.isArray() ? clazz.getCanonicalName() : clazz.getName();
        }

        @Override
        public boolean matches(Object o) {
            if (null == o
                    || !(o instanceof GreysProtocol)) {
                return false;
            }
            final GreysProtocol<ClassInfo> gpClassInfo = (GreysProtocol<ClassInfo>) o;
            final ClassInfo classInfo = gpClassInfo.getContent();

            assertEquals("matching class name", getClassName(), classInfo.getName());
            assertNotNull("matching code source", classInfo.getCodeSource());
            assertEquals("matching annotation's size", clazz.getDeclaredAnnotations().length, classInfo.getAnnotationTypes().size());
            assertEquals("matching interface's size", clazz.getInterfaces().length, classInfo.getInterfaceTypes().size());
            if (clazz.getSuperclass() != null) {
                assertEquals(
                        "matching superclass",
                        clazz.getSuperclass().getName(),
                        classInfo.getFamilyTypes().get(0)
                );
            } else {
                assertEquals("matching superclass", 0, classInfo.getFamilyTypes().size());
            }
            if (clazz.getClassLoader() != null) {
                assertEquals(
                        "matching classloader",
                        clazz.getClassLoader().getClass().getName(),
                        classInfo.getClassLoaders().get(0)
                );
            } else {
                assertEquals("matching classloader", 0, classInfo.getClassLoaders().size());
            }
            assertEquals("matching modifiers", clazz.getModifiers(), classInfo.getModifier());

            assertEquals("matching isInterface",
                    clazz.isInterface(),
                    (classInfo.getClassType() & CLASS_TYPE_INTERFACE) == CLASS_TYPE_INTERFACE
            );

            assertEquals("matching isAnnotation",
                    clazz.isAnnotation(),
                    (classInfo.getClassType() & CLASS_TYPE_ANNOTATION) == CLASS_TYPE_ANNOTATION
            );

            assertEquals("matching isEnum",
                    clazz.isEnum(),
                    (classInfo.getClassType() & CLASS_TYPE_ENUM) == CLASS_TYPE_ENUM
            );

            assertEquals("matching isAnonymousClass",
                    clazz.isAnonymousClass(),
                    (classInfo.getClassType() & CLASS_TYPE_ANONYMOUS) == CLASS_TYPE_ANONYMOUS
            );

            assertEquals("matching isArray",
                    clazz.isArray(),
                    (classInfo.getClassType() & CLASS_TYPE_ARRAY) == CLASS_TYPE_ARRAY
            );

            assertEquals("matching isLocalClass",
                    clazz.isLocalClass(),
                    (classInfo.getClassType() & CLASS_TYPE_LOCAL) == CLASS_TYPE_LOCAL
            );

            assertEquals("matching isMemberClass",
                    clazz.isMemberClass(),
                    (classInfo.getClassType() & CLASS_TYPE_MEMBER) == CLASS_TYPE_MEMBER
            );

            assertEquals("matching isPrimitive",
                    clazz.isPrimitive(),
                    (classInfo.getClassType() & CLASS_TYPE_PRIMITIVE) == CLASS_TYPE_PRIMITIVE
            );

            assertEquals("matching isSynthetic",
                    clazz.isSynthetic(),
                    (classInfo.getClassType() & CLASS_TYPE_SYNTHETIC) == CLASS_TYPE_SYNTHETIC
            );


            return true;
        }
    }

}
