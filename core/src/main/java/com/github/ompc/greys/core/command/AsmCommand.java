package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.affect.RowAffect;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.util.Set;

import static com.github.ompc.greys.core.util.SearchUtils.searchClassWithSubClass;

/**
 * clone from {@link TraceClassVisitor}
 */
class GaTraceClassVisitor extends ClassVisitor {

    /**
     * The print writer to be used to print the class. May be null.
     */
    private final PrintWriter pw;

    /**
     * The object that actually converts visit events into text.
     */
    public final Printer p;

    /**
     * Constructs a new {@link GaTraceClassVisitor}.
     *
     * @param pw the print writer to be used to print the class.
     */
    public GaTraceClassVisitor(final PrintWriter pw) {
        super(Opcodes.ASM5, null);
        this.pw = pw;
        this.p = new Textifier();
    }

    @Override
    public void visit(final int version, final int access, final String name,
                      final String signature, final String superName,
                      final String[] interfaces) {
        p.visit(version, access, name, signature, superName, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(final String file, final String debug) {
        p.visitSource(file, debug);
        super.visitSource(file, debug);
    }

    @Override
    public void visitOuterClass(final String owner, final String name,
                                final String desc) {
        p.visitOuterClass(owner, name, desc);
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
                                             final boolean visible) {
        Printer p = this.p.visitClassAnnotation(desc, visible);
        AnnotationVisitor av = cv == null ? null : cv.visitAnnotation(desc,
                visible);
        return new TraceAnnotationVisitor(av, p);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef,
                                                 TypePath typePath, String desc, boolean visible) {
        Printer p = this.p.visitClassTypeAnnotation(typeRef, typePath, desc,
                visible);
        AnnotationVisitor av = cv == null ? null : cv.visitTypeAnnotation(
                typeRef, typePath, desc, visible);
        return new TraceAnnotationVisitor(av, p);
    }

    @Override
    public void visitAttribute(final Attribute attr) {
        p.visitClassAttribute(attr);
        super.visitAttribute(attr);
    }

    @Override
    public void visitInnerClass(final String name, final String outerName,
                                final String innerName, final int access) {
        p.visitInnerClass(name, outerName, innerName, access);
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name,
                                   final String desc, final String signature, final Object value) {
        Printer p = this.p.visitField(access, name, desc, signature, value);
        FieldVisitor fv = cv == null ? null : cv.visitField(access, name, desc,
                signature, value);
        return new TraceFieldVisitor(fv, p);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
                                     final String desc, final String signature, final String[] exceptions) {
        Printer p = this.p.visitMethod(access, name, desc, signature,
                exceptions);
        MethodVisitor mv = cv == null ? null : cv.visitMethod(access, name,
                desc, signature, exceptions);
        return new TraceMethodVisitor(mv, p);
    }

    @Override
    public void visitEnd() {
        p.visitClassEnd();
        if (pw != null) {
            p.print(pw);
            pw.flush();
        }
        super.visitEnd();
    }

}

/**
 * 查看指定类的字节码
 * Created by oldmanpushcart@gmail.com on 15/12/10.
 */
@Cmd(name = "asm", sort = 11, summary = "Display class bytecode by asm format",
        eg = {
                "asm -f java.lang.String",
                "asm -f *StringUtils",
                "asm *StringUtils isEmpty"
        })
public class AsmCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, isRequired = false, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "f", summary = "Display all the member variables")
    private boolean isField = false;


    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final RowAffect affect = new RowAffect();
                final StringBuilder outputSB = new StringBuilder();

                // 找到所有匹配的类
                final Matcher classNameMatcher = new Matcher.PatternMatcher(isRegEx, classPattern);

                final Matcher methodNameMatcher;
                if (StringUtils.isBlank(methodPattern)) {
                    methodNameMatcher = new Matcher.TrueMatcher();
                } else {
                    methodNameMatcher = new Matcher.PatternMatcher(isRegEx, methodPattern);
                }

                final Set<Class<?>> matchedClassSet = searchClassWithSubClass(inst, classNameMatcher);

                for (Class<?> clazz : matchedClassSet) {

                    // 不是所有类都能看字节码的
                    if (clazz.isArray()) {
                        continue;
                    }

                    final ClassLoader classLoader = clazz.getClassLoader();
                    final String title = String.format("asm bytecode for \"%s\" @ClassLoader:%s",
                            clazz.getName(),
                            classLoader
                    );

                    final String resourceName = "/" + StringUtils.replace(clazz.getName(), ".", "/") + ".class";

                    InputStream is = null;
                    final StringWriter writer = new StringWriter();
                    try {
                        is = clazz.getResourceAsStream(resourceName);
                        if (null != is) {
                            final ClassReader cr = new ClassReader(is);
                            final GaTraceClassVisitor trace = new GaTraceClassVisitor(new PrintWriter(writer, true)) {

                                @Override
                                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                                    if (isField) {
                                        return super.visitField(access, name, desc, signature, value);
                                    } else {
                                        return null;
                                    }
                                }

                                @Override
                                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                                    if (methodNameMatcher.matching(name)) {
                                        return super.visitMethod(access, name, desc, signature, exceptions);
                                    } else {
                                        return null;
                                    }
                                }

                            };
                            cr.accept(trace, ClassReader.SKIP_DEBUG);
                            outputSB.append(title).append("\n").append(writer.toString()).append("\n");
                            affect.rCnt(1);
                        }
                    } finally {
                        IOUtils.closeQuietly(writer);
                        IOUtils.closeQuietly(is);
                    }

                }

                printer.print(outputSB.toString()).finish();
                return affect;
            }
        };
    }

}
