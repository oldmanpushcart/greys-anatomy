package com.github.ompc.greys.core.util.affect;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * Asm命令特殊返回
 * Created by vlinux on 16/1/7.
 */
public final class AsmAffect extends RowAffect {

    private final List<ClassInfo> classInfos = new ArrayList<ClassInfo>();


    /**
     * 获取类信息集合
     *
     * @return 类信息集合
     */
    public List<ClassInfo> getClassInfos() {
        return classInfos;
    }

    /**
     * 信息
     */
    public static class ClassInfo {

        private final Class<?> clazz;
        private final ClassLoader loader;
        private final byte[] byteArray;
        private final ProtectionDomain protectionDomain;

        public ClassInfo(Class<?> clazz, ClassLoader loader, byte[] byteArray, ProtectionDomain protectionDomain) {
            this.clazz = clazz;
            this.loader = loader;
            this.byteArray = byteArray;
            this.protectionDomain = protectionDomain;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public ClassLoader getLoader() {
            return loader;
        }

        public byte[] getByteArray() {
            return byteArray;
        }

        public ProtectionDomain getProtectionDomain() {
            return protectionDomain;
        }
    }

}
