/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.quarkus.deployment.nativebuild;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

import io.quarkus.gizmo.Gizmo;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Patches Atmosphere {@code InjectableObjectFactory} to remove the
 * {@code injectableServiceLoader} field and its initialization that is causing
 * issue during STATIC_INIT phase in native build, and replaces field usages
 * with a direct calls to {@code ServiceLoader.load(Injectable.class)}.
 */
final class InjectableObjectFactoryFieldUsageRemovalMethodVisitor
        extends MethodNode {
    public InjectableObjectFactoryFieldUsageRemovalMethodVisitor(
            MethodVisitor mv, int access, String name, String descriptor,
            String signature, String[] exceptions) {
        super(Gizmo.ASM_API_VERSION, access, name, descriptor, signature,
                exceptions);
        this.mv = mv;
    }

    @Override
    public void visitEnd() {
        ListIterator<AbstractInsnNode> iterator = instructions.iterator();
        Map<AbstractInsnNode, AbstractInsnNode> nodesToReplace = new HashMap<>();

        FieldInsnNode fieldUsage;
        while ((fieldUsage = findFieldUsage(iterator)) != null) {
            if (fieldUsage.getOpcode() == Opcodes.PUTFIELD) {
                /*
                 * Remove injectableServiceLoader initialization
                 *
                 * L6 LINENUMBER 66 L6 ALOAD 0 LDC
                 * Lorg/atmosphere/inject/Injectable;.class INVOKESTATIC
                 * java/util/ServiceLoader.load
                 * (Ljava/lang/Class;)Ljava/util/ServiceLoader; PUTFIELD
                 * org/atmosphere/inject/InjectableObjectFactory.
                 * injectableServiceLoader : Ljava/util/ServiceLoader;
                 */
                nodesToReplace.put(iterator.previous(),
                        new InsnNode(Opcodes.NOP)); // PUTFIELD
                nodesToReplace.put(iterator.previous(),
                        new InsnNode(Opcodes.NOP)); // INVOKESTATIC
                nodesToReplace.put(iterator.previous(),
                        new InsnNode(Opcodes.NOP)); // LDC
                nodesToReplace.put(iterator.previous(),
                        new InsnNode(Opcodes.NOP)); // ALOAD

                AbstractInsnNode stop = fieldUsage;
                findNextNode(iterator, n -> n == stop);
            } else if (fieldUsage.getOpcode() == Opcodes.GETFIELD) {
                /*
                 * Replaces usage of injectableServiceLoader field with a call
                 * to ServiceLoader.load(Injectable.class)
                 *
                 * L13 LINENUMBER 86 L13 FRAME FULL
                 * [org/atmosphere/inject/InjectableObjectFactory
                 * org/atmosphere/cpr/AtmosphereConfig java/lang/String] []
                 * ALOAD 0 GETFIELD
                 * org/atmosphere/inject/InjectableObjectFactory.
                 * injectableServiceLoader : Ljava/util/ServiceLoader;
                 * INVOKEVIRTUAL java/util/ServiceLoader.iterator
                 * ()Ljava/util/Iterator; ASTORE 3
                 */
                nodesToReplace.put(iterator.previous(), new MethodInsnNode(
                        Opcodes.INVOKESTATIC, "java/util/ServiceLoader", "load",
                        "(Ljava/lang/Class;)Ljava/util/ServiceLoader;", false)); // GETFIELD
                nodesToReplace.put(iterator.previous(), new LdcInsnNode(
                        Type.getType("Lorg/atmosphere/inject/Injectable;"))); // ALOAD
                AbstractInsnNode stop = fieldUsage;
                findNextNode(iterator, n -> n == stop);
            }
        }
        nodesToReplace.forEach(instructions::set);
        instructions.resetLabels();
        accept(mv);
    }

    private FieldInsnNode findFieldUsage(
            ListIterator<AbstractInsnNode> iterator) {
        return (FieldInsnNode) findNextNode(iterator,
                node -> node instanceof FieldInsnNode field
                        && field.name.equals("injectableServiceLoader"));
    }

    private AbstractInsnNode findNextNode(
            ListIterator<AbstractInsnNode> iterator,
            Predicate<AbstractInsnNode> test) {
        while (iterator.hasNext()) {
            AbstractInsnNode node = iterator.next();
            if (test.test(node)) {
                return node;
            }
        }
        return null;
    }
}
