package com.github.ompc.greys.agent;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Agent ClassLoader
 * Created by vlinux on 2016/11/7.
 */
public class AgentClassLoader extends URLClassLoader {


    public AgentClassLoader(final String agentJar) throws MalformedURLException {
        super(new URL[]{new URL("file:" + agentJar)});
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            return super.loadClass(name, resolve);
        }
    }

}
