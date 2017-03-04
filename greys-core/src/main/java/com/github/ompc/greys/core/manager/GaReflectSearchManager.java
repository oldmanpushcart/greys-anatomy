package com.github.ompc.greys.core.manager;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.github.ompc.greys.core.GaMethod;

import java.util.Set;

/**
 * Greys封装的反射查询
 * Created by vlinux on 2017/3/2.
 */
public interface GaReflectSearchManager {

    Set<GaMethod> listVisualGaMethods(String patternJavaClassName,
                                      String patternJavaMethodName,
                                      boolean isIncludeSubClass);

}
