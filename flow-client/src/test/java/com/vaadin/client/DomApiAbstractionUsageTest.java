package com.vaadin.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.vaadin.client.bootstrap.Bootstrapper;
import com.vaadin.client.flow.RouterLinkHandler;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.dom.DomElement;
import com.vaadin.client.flow.dom.DomNode;

import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.Text;
import elemental.html.AnchorElement;

public class DomApiAbstractionUsageTest {
    private static final Set<String> ignoredClasses = Stream
            .of(DomElement.class, DomNode.class, ResourceLoader.class,
                    BrowserInfo.class, SystemErrorHandler.class,
                    RouterLinkHandler.class, Profiler.class,
                    ScrollPositionHandler.class)
            .map(Class::getName).collect(Collectors.toSet());

    private static final Set<Class<?>> ignoredElementalClasses = Stream
            .of(Document.class, AnchorElement.class, Text.class)
            .collect(Collectors.toSet());

    private static final Set<String> ignoredElementMethods = Stream
            .of("getTagName", "addEventListener", "getOwnerDocument",
                    "hasAttribute", "getStyle", "getLocalName", "getAttribute",
                    "equals", "getClass")
            .collect(Collectors.toSet());

    private final ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5) {
        private boolean whitelistedClass = false;
        private String className;

        @Override
        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
            className = name.replace('/', '.');

            String outerClassName = className.replaceAll("\\$.*", "");
            whitelistedClass = ignoredClasses.contains(outerClassName);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc,
                String signature, Object value) {
            // Trim array markers (not efficient, but straightforward)
            while (desc.startsWith("[")) {
                desc = desc.substring(1);
            }

            if (desc.startsWith("L")) {
                // Lcom/foo/Foo;
                String typeName = desc.substring(1, desc.length() - 1);
                Class<?> type = DomApiAbstractionUsageTest.getClass(typeName);
                if (DomNode.class.isAssignableFrom(type)) {
                    Assert.fail(className + "." + name
                            + " references a wrapped node");
                }
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName,
                String desc, String signature, String[] exceptions) {
            if (whitelistedClass) {
                return null;
            }

            return new MethodVisitor(api) {
                @Override
                public void visitMethodInsn(int opcode, String targetClass,
                        String targetMethod, String targetDesc,
                        boolean inInterface) {
                    verifyMethod(className + "." + methodName, targetClass,
                            targetMethod);
                }
            };
        }
    };

    /**
     * This tests that no API from {@link DomElement} or {@link DomNode} is used
     * without wrapping it with a {@link DomApi#wrap(elemental.dom.Node)} call.
     */
    @Test
    public void testDomApiCodeNotUsed() throws IOException {
        String classesPath = getClassesLocation(Bootstrapper.class);

        Files.walk(Paths.get(classesPath))
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(this::testClassFile);
    }

    private void testClassFile(Path classFile) {
        try (InputStream stream = new FileInputStream(classFile.toString())) {
            ClassReader classReader = new ClassReader(stream);

            int flags = 0;
            classReader.accept(classVisitor, flags);
        } catch (IOException e) {
            throw new RuntimeException(classFile.toString(), e);
        }
    }

    private static void verifyMethod(String callingMethod,
            String targetClassName, String targetMethod) {
        // Won't care about overhead of loading all
        // classes since this is just a test
        Class<?> targetClass = getClass(targetClassName);

        if (!Node.class.isAssignableFrom(targetClass)) {
            return;
        }

        if (ignoredElementalClasses.contains(targetClass)) {
            return;
        }

        if ((Element.class == targetClass || Node.class == targetClass)
                && ignoredElementMethods.contains(targetMethod)) {
            return;
        }

        Assert.fail(callingMethod + " calls " + targetClass.getName() + "."
                + targetMethod);
    }

    private static Class<?> getClass(String targetClassName) {
        try {
            return Class.forName(targetClassName.replace('/', '.'), false,
                    DomApiAbstractionUsageTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(targetClassName, e);
        }
    }

    private String getClassesLocation(Class<Bootstrapper> sampleClass) {
        String sampleClassName = '/' + sampleClass.getName().replace('.', '/')
                + ".class";

        URL sampleClassLocation = sampleClass.getResource(sampleClassName);

        assert "file".equals(sampleClassLocation.getProtocol());

        String sampleClassAbsolutePath;
        try {
            sampleClassAbsolutePath = Paths.get(sampleClassLocation.toURI())
                    .toFile().getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        assert sampleClassAbsolutePath
                .endsWith(sampleClassName.replace('/', File.separatorChar));

        return sampleClassAbsolutePath.substring(0,
                sampleClassAbsolutePath.length() - sampleClassName.length());
    }
}
