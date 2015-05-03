package com.googlecode.greysanatomy.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.CodeSource;

import static java.lang.String.format;

public class GaDetailUtils {


    private static final String STEP_TAB = "                    ";
    private static final String STEP_FLOW_TAB = "  ";
    private static final String FLOW_TAB = "                  ";
    private static final String NULL = GaStringUtils.EMPTY;

    public static String detail(Class<?> clazz, boolean isShowFields) {

        StringBuilder detailSB = new StringBuilder();
        detailSB.append("class info : ").append(getClassName(clazz)).append("\n");
        detailSB.append(GaStringUtils.LINE);

        CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        detailSB.append(format("%16s : %s\n", "code-source", null == cs ? NULL : cs.getLocation().getFile()));
        detailSB.append(format("%16s : %s\n", "name", getClassName(clazz)));
        detailSB.append(format("%16s : %s\n", "isInterface", clazz.isInterface()));
        detailSB.append(format("%16s : %s\n", "isAnnotation", clazz.isAnnotation()));
        detailSB.append(format("%16s : %s\n", "isEnum", clazz.isEnum()));
        detailSB.append(format("%16s : %s\n", "isAnonymousClass", clazz.isAnonymousClass()));
        detailSB.append(format("%16s : %s\n", "isArray", clazz.isArray()));
        detailSB.append(format("%16s : %s\n", "isLocalClass", clazz.isLocalClass()));
        detailSB.append(format("%16s : %s\n", "isMemberClass", clazz.isMemberClass()));
        detailSB.append(format("%16s : %s\n", "isPrimitive", clazz.isPrimitive()));
        detailSB.append(format("%16s : %s\n", "isSynthetic", clazz.isSynthetic()));
        detailSB.append(format("%16s : %s\n", "simple-name", clazz.getSimpleName()));
        detailSB.append(format("%16s : %s\n", "modifier", tranModifier(clazz.getModifiers())));

        // annotation
        {
            StringBuilder annoSB = new StringBuilder();
            Annotation[] annos = clazz.getDeclaredAnnotations();
            if (null != annos && annos.length > 0) {
                for (Annotation anno : annos) {
                    annoSB.append(getClassName(anno.annotationType())).append(",");
                }
                if (annoSB.length() > 0) {
                    annoSB.deleteCharAt(annoSB.length() - 1);
                }
            } else {
                annoSB.append(NULL);
            }
            detailSB.append(format("%16s : %s\n", "annotation", annoSB.toString()));
        }

        // interface
        {
            StringBuilder interfaceSB = new StringBuilder();
            Class<?>[] interfaces = clazz.getInterfaces();
            if (null == interfaces || interfaces.length == 0) {
                interfaceSB.append(NULL);
            } else {
                for (Class<?> i : interfaces) {
                    interfaceSB.append(i.getName()).append(",");
                }
                if (interfaceSB.length() > 0) {
                    interfaceSB.deleteCharAt(interfaceSB.length() - 1);
                }
//                interfaceSB.append("\n");
            }
            detailSB.append(format("%16s : %s\n", "interfaces", interfaceSB.toString()));
        }

        // super-class
        {
            StringBuilder preSB = new StringBuilder();
            Class<?> superClass = clazz.getSuperclass();
            if (null != superClass) {
                StringBuilder superSB = new StringBuilder(getClassName(superClass)).append("\n");
                while (true) {
                    superClass = superClass.getSuperclass();
                    if (null == superClass) {
                        break;
                    }
                    superSB.append(STEP_TAB).append(preSB.toString()).append("`-->").append(getClassName(superClass)).append("\n");
                    preSB.append(STEP_FLOW_TAB);
                }//while
                detailSB.append(format("%16s : %s", "super-class", superSB.toString()));
            } else {
                detailSB.append(format("%16s : %s\n", "super-class", NULL));
            }

        }

        // class-loader
        {
            StringBuilder preSB = new StringBuilder();
            StringBuilder loaderSB = new StringBuilder();
            ClassLoader loader = clazz.getClassLoader();
            if (null != loader) {
                loaderSB.append(loader.toString()).append("\n");
                while (true) {
                    loader = loader.getParent();
                    if (null == loader) {
                        break;
                    }
                    loaderSB.append(STEP_TAB).append(preSB.toString()).append("`-->").append(loader.toString()).append("\n");
                    preSB.append(STEP_FLOW_TAB);
                }
            } else {
                loaderSB.append(NULL).append("\n");
            }//if
            detailSB.append(format("%16s : %s", "class-loader", loaderSB.toString()));
        }


        // field
        if (isShowFields) {
            StringBuilder fieldSB = new StringBuilder();
            Field[] fields = clazz.getDeclaredFields();
            if (null != fields
                    && fields.length > 0) {

                for (Field field : fields) {

                    fieldSB.append("\n");
                    fieldSB.append(format("%24s : %s\n", "name", field.getName()));
                    fieldSB.append(format("%24s : %s\n", "type", field.getType()));
                    fieldSB.append(format("%24s : %s\n", "modifier", tranModifier(field.getModifiers())));

                    StringBuilder annoSB = new StringBuilder();
                    Annotation[] annos = field.getAnnotations();
                    if (null != annos && annos.length > 0) {
                        for (Annotation anno : annos) {
                            annoSB.append(getClassName(anno.annotationType())).append(",");
                        }
                        if (annoSB.length() > 0) {
                            annoSB.deleteCharAt(annoSB.length() - 1);
                        }
                        fieldSB.append(format("%24s : %s\n", "annotation", annoSB.toString()));
                    }


                    if (Modifier.isStatic(field.getModifiers())) {
                        // final boolean isAccessible =  field.isAccessible();
                        try {
                            field.setAccessible(true);
                            fieldSB.append(format("%24s : %s\n", "value", field.get(null)));
                        } catch (IllegalAccessException e) {
                            //
                        }
                        //finally {
                        // field.setAccessible(isAccessible);
                        //}
                    }//if

                }//for

                detailSB.append(format("%16s : %s", "fields", fieldSB.toString()));

            }

        }

        return detailSB.toString();

    }

    private static String tranModifier(int mod) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isAbstract(mod)) {
            sb.append("abstract,");
        }
        if (Modifier.isFinal(mod)) {
            sb.append("final,");
        }
        if (Modifier.isInterface(mod)) {
            sb.append("interface,");
        }
        if (Modifier.isNative(mod)) {
            sb.append("native,");
        }
        if (Modifier.isPrivate(mod)) {
            sb.append("private,");
        }
        if (Modifier.isProtected(mod)) {
            sb.append("protected,");
        }
        if (Modifier.isPublic(mod)) {
            sb.append("public,");
        }
        if (Modifier.isStatic(mod)) {
            sb.append("static,");
        }
        if (Modifier.isStrict(mod)) {
            sb.append("strict,");
        }
        if (Modifier.isSynchronized(mod)) {
            sb.append("synchronized,");
        }
        if (Modifier.isTransient(mod)) {
            sb.append("transient,");
        }
        if (Modifier.isVolatile(mod)) {
            sb.append("volatile,");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String detail(Method method) {
        StringBuilder detailSB = new StringBuilder();
        detailSB.append("method info : ").append(format("%s->%s", method.getDeclaringClass().getName(), method.getName())).append("\n");
        detailSB.append(GaStringUtils.LINE);

        detailSB.append(format("%16s : %s\n", "declaring-class", getClassName(method.getDeclaringClass())));
        detailSB.append(format("%16s : %s\n", "modifier", tranModifier(method.getModifiers())));
        detailSB.append(format("%16s : %s\n", "name", method.getName()));

        // annotation
        {
            StringBuilder annoSB = new StringBuilder();
            Annotation[] annos = method.getDeclaredAnnotations();
            if (null != annos && annos.length > 0) {
                for (Annotation anno : annos) {
                    annoSB.append(anno.annotationType().getName()).append(",");
                }
                if (annoSB.length() > 0) {
                    annoSB.deleteCharAt(annoSB.length() - 1);
                }
            } else {
                annoSB.append(NULL);
            }
            detailSB.append(format("%16s : %s\n", "annotation", annoSB.toString()));
        }

        detailSB.append(format("%16s : %s\n", "return-type", getClassName(method.getReturnType())));

        // params
        {
            StringBuilder paramSB = new StringBuilder();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (null != paramTypes && paramTypes.length > 0) {
                boolean isFirst = true;
                for (Class<?> clazz : paramTypes) {
                    paramSB.append(isFirst ? "" : FLOW_TAB).append(getClassName(clazz)).append("\n");
                    isFirst = false;
                }
                if (paramSB.length() > 0) {
                    paramSB.deleteCharAt(paramSB.length() - 1);
                }
            } else {
                paramSB.append(NULL);
            }
            detailSB.append(format("%16s : %s\n", "params", paramSB.toString()));
        }

        return detailSB.toString();
    }

    private static String getClassName(Class<?> clazz) {
        if (clazz.isArray()) {
            StringBuilder sb = new StringBuilder(clazz.getName());
            sb.delete(0, 2);
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ';') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("[]");
            return sb.toString();
        } else {
            return clazz.getName();
        }
    }

}
