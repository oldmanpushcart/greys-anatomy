package com.github.ompc.greys.module.util;

import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

/**
 * 资源注入构造器
 *
 * @param <T> 需要注入的对象类型
 */
public class InjectResourceBuilder<T> {

    private final T target;
    private final Set<Field> resourceFields = new HashSet<Field>();

    /**
     * 构造资源注入构造器
     *
     * @param target 需要注入资源的目标对象
     */
    public InjectResourceBuilder(T target) {
        this.target = target;
        this.resourceFields.addAll(getFieldsListWithAnnotation(
                target.getClass(),
                Resource.class
        ));
    }

    /**
     * 给资源属性注入资源对象
     *
     * @param resource 资源对象
     * @return this
     * @throws IllegalAccessException 属性访问失败(注入失败)
     */
    public InjectResourceBuilder<T> inject(final Object resource) throws IllegalAccessException {
        for (final Field field : resourceFields) {
            if (field.getType().isAssignableFrom(resource.getClass())) {
                writeField(field, target, resource, true);
            }
        }
        return this;
    }

    /**
     * 构造注入好的对象
     *
     * @return 注入好的对象
     */
    public T build() {
        return target;
    }

}
