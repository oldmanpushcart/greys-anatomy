package com.github.ompc.greys.core;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.filter.OrGroupFilter;
import com.github.ompc.greys.core.GaMethod;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Greys沙箱匹配器
 * Created by vlinux on 2017/2/11.
 */
public class GaFilter implements Filter {

    private final OrGroupFilter orGroupFilter;

    public GaFilter(final Set<GaMethod> gaMethods) {
        this.orGroupFilter = toOrGroupFilter(gaMethods);
    }

    private OrGroupFilter toOrGroupFilter(Set<GaMethod> gaMethods) {
        // step1. 找出所有匹配类（可能包含子类）及其所有可见方法（可能包含可见方法声明类）
        final Map<String/*JavaClassName*/, Set<String/*JavaMethodName*/>> classMultimap = new HashMap<String, Set<String>>();
        for (final GaMethod gaMethod : gaMethods) {
            if (null == gaMethod
                    || null == gaMethod.getDeclaringClass()) {
                continue;
            }
            final String declaringJavaClassName = gaMethod.getDeclaringClass().getName();
            final String javaMethodName = gaMethod.getName();
            if (classMultimap.containsKey(declaringJavaClassName)) {
                classMultimap.get(declaringJavaClassName).add(javaMethodName);
            } else {
                final Set<String> javaMethodNameSet = new HashSet<String>();
                javaMethodNameSet.add(javaMethodName);
                classMultimap.put(declaringJavaClassName, javaMethodNameSet);
            }
        }

        // step2. 对所有GaMethod成员进行重新分组
        final List<Filter> filters = new ArrayList<Filter>();
        for (final Map.Entry<String, Set<String>> entry : classMultimap.entrySet()) {
            filters.add(new Filter() {
                @Override
                public boolean doClassFilter(final int access,
                                             final String javaClassName,
                                             final String superClassTypeJavaClassName,
                                             final String[] interfaceTypeJavaClassNameArray,
                                             final String[] annotationTypeJavaClassNameArray) {
                    return StringUtils.equals(entry.getKey(), javaClassName);
                }

                @Override
                public boolean doMethodFilter(final int access,
                                              final String javaMethodName,
                                              final String[] parameterTypeJavaClassNameArray,
                                              final String[] throwsTypeJavaClassNameArray,
                                              final String[] annotationTypeJavaClassNameArray) {
                    return entry.getValue().contains(javaMethodName);
                }
            });
        }

        // step3. 拼接到分组匹配过滤器中
        return new OrGroupFilter(filters.toArray(new Filter[]{}));
    }

    @Override
    public boolean doClassFilter(final int access,
                                 final String javaClassName,
                                 final String superClassTypeJavaClassName,
                                 final String[] interfaceTypeJavaClassNameArray,
                                 final String[] annotationTypeJavaClassNameArray) {
        return orGroupFilter.doClassFilter(
                access,
                javaClassName,
                superClassTypeJavaClassName,
                interfaceTypeJavaClassNameArray,
                annotationTypeJavaClassNameArray
        );
    }

    @Override
    public boolean doMethodFilter(final int access,
                                  final String javaMethodName,
                                  final String[] parameterTypeJavaClassNameArray,
                                  final String[] throwsTypeJavaClassNameArray,
                                  final String[] annotationTypeJavaClassNameArray) {
        return orGroupFilter.doMethodFilter(
                access,
                javaMethodName,
                parameterTypeJavaClassNameArray,
                throwsTypeJavaClassNameArray,
                annotationTypeJavaClassNameArray
        );
    }

}
