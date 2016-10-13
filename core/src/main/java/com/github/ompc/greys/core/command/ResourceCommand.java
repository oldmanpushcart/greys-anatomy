package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 资源查询命令
 * Created by vlinux on 16/9/24.
 */
//@Cmd(name = "resource", sort = 8, summary = "Finds the resource with the given name",
//        eg = {
//                "resource *spring-manager.xml"
//        })
// TODO : 尚未想好如何输出格式内容,有待后续完善
public class ResourceCommand implements Command {

    @IndexArg(index = 0, name = "resource-name", summary = "Path and resource of Pattern Matching")
    private String resourceName;

    @Override
    public Command.Action getAction() {
        return new Command.RowAction() {

            // 列出所有已经被加载的ClassLoader
            private Set<ClassLoader> listLoadedClassLoader(Instrumentation inst) {
                final Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();
                for (Class<?> clazz : inst.getAllLoadedClasses()) {
                    if (null == clazz) {
                        continue;
                    }
                    final ClassLoader loader = clazz.getClassLoader();
                    if (null != loader) {
                        classLoaderSet.add(loader);
                    }
                }
                return classLoaderSet;
            }

            private Map<ClassLoader, URL> searchResourceMapByName(final Set<ClassLoader> classLoaderSet,
                                                                  final String resourceName) {
                final Map<ClassLoader, URL> classLoaderResourceMapping = new HashMap<ClassLoader, URL>();
                for (final ClassLoader loader : classLoaderSet) {
                    final URL resourceURL = loader.getResource(resourceName);
                    if (null != resourceURL
                            && !classLoaderResourceMapping.containsKey(loader)) {
                        classLoaderResourceMapping.put(loader, resourceURL);
                    }
                }
                return classLoaderResourceMapping;
            }

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                final Map<ClassLoader, URL> resourceMap = searchResourceMapByName(listLoadedClassLoader(inst), resourceName);
                for (final Map.Entry<ClassLoader, URL> entry : resourceMap.entrySet()) {

                    final String title = String.format("// RESOURCE INFORMATION FOR \"%s\" @ClassLoader:%s",
                            entry.getValue().toString(),
                            entry.getKey()
                    );


                }
                return new RowAffect(resourceMap.size());
            }
        };
    }

}
