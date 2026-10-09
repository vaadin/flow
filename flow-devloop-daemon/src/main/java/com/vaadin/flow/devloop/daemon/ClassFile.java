/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.devloop.daemon;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * As much of a class file as the daemon asks about: which strings its constant
 * pool holds, whether it declares a {@code public static void main}, and which
 * source file it was compiled from.
 * <p>
 * Hand-parsed because the alternative is loading the class, which would mean
 * the application's classpath in the daemon JVM - the one thing this module
 * exists to avoid. The bytecode is skipped.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param strings
 *            the UTF-8 entries of the constant pool, indexed as the pool is
 * @param hasMainMethod
 *            whether it declares {@code public static void main(String[])}
 * @param sourceFile
 *            the file name its {@code SourceFile} attribute records, such as
 *            {@code View.java}, or {@code null} when it was compiled without
 *            one
 */
record ClassFile(List<String> strings, boolean hasMainMethod,
        String sourceFile) {

    private static final String MAIN_DESCRIPTOR = "([Ljava/lang/String;)V";

    private static final int ACC_PUBLIC = 0x0001;
    private static final int ACC_STATIC = 0x0008;

    /**
     * Reads a class file.
     *
     * @param file
     *            the class file
     * @return what it holds, or empty when it is unreadable, truncated or not a
     *         class file
     */
    static Optional<ClassFile> read(Path file) {
        try (InputStream in = Files.newInputStream(file);
                DataInputStream data = new DataInputStream(in)) {
            if (data.readInt() != 0xCAFEBABE) {
                return Optional.empty();
            }
            data.readUnsignedShort(); // minor version
            data.readUnsignedShort(); // major version
            List<String> strings = readConstantPool(data);
            data.readUnsignedShort(); // access flags
            data.readUnsignedShort(); // this class
            data.readUnsignedShort(); // super class
            data.skipBytes(data.readUnsignedShort() * 2); // interfaces
            skipMembers(data); // fields
            boolean hasMainMethod = readMethods(data, strings);
            return Optional.of(new ClassFile(strings, hasMainMethod,
                    readSourceFile(data, strings)));
        } catch (IOException | RuntimeException e) {
            // A truncated or unsupported class file answers no question; the
            // caller treats it as unknown.
            return Optional.empty();
        }
    }

    /**
     * The UTF-8 entries of the constant pool, indexed as the pool is - long and
     * double entries take two slots, which is what makes a blind walk
     * impossible and this method necessary.
     */
    private static List<String> readConstantPool(DataInputStream data)
            throws IOException {
        int count = data.readUnsignedShort();
        List<String> strings = new ArrayList<>(count);
        strings.add(""); // the pool is 1-based
        for (int index = 1; index < count; index++) {
            int tag = data.readUnsignedByte();
            switch (tag) {
            case 1 -> strings.add(data.readUTF());
            case 7, 8, 16, 19, 20 -> {
                data.skipBytes(2);
                strings.add("");
            }
            case 15 -> {
                data.skipBytes(3);
                strings.add("");
            }
            case 3, 4, 9, 10, 11, 12, 17, 18 -> {
                data.skipBytes(4);
                strings.add("");
            }
            case 5, 6 -> {
                data.skipBytes(8);
                strings.add("");
                // A long or a double occupies the following slot as well.
                strings.add("");
                index++;
            }
            default ->
                throw new IOException("unsupported constant pool tag " + tag);
            }
        }
        return strings;
    }

    /**
     * Walks the whole method table, so that the class attributes after it are
     * read from where they start.
     *
     * @return whether one of the methods is a {@code public static void main}
     */
    private static boolean readMethods(DataInputStream data,
            List<String> strings) throws IOException {
        boolean hasMainMethod = false;
        int methods = data.readUnsignedShort();
        for (int i = 0; i < methods; i++) {
            int flags = data.readUnsignedShort();
            String name = at(strings, data.readUnsignedShort());
            String descriptor = at(strings, data.readUnsignedShort());
            skipAttributes(data);
            hasMainMethod |= "main".equals(name)
                    && MAIN_DESCRIPTOR.equals(descriptor)
                    && (flags & ACC_PUBLIC) != 0 && (flags & ACC_STATIC) != 0;
        }
        return hasMainMethod;
    }

    private static String readSourceFile(DataInputStream data,
            List<String> strings) throws IOException {
        int count = data.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            String name = at(strings, data.readUnsignedShort());
            int length = data.readInt();
            if ("SourceFile".equals(name) && length == 2) {
                return at(strings, data.readUnsignedShort());
            }
            skipFully(data, length);
        }
        return null;
    }

    private static String at(List<String> strings, int index) {
        return index >= 0 && index < strings.size() ? strings.get(index) : "";
    }

    private static void skipMembers(DataInputStream data) throws IOException {
        int count = data.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            data.skipBytes(6); // access flags, name, descriptor
            skipAttributes(data);
        }
    }

    private static void skipAttributes(DataInputStream data)
            throws IOException {
        int count = data.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            data.skipBytes(2); // name index
            skipFully(data, data.readInt());
        }
    }

    /**
     * skipBytes is allowed to skip fewer, and for a Code attribute read from a
     * stream it does; looping is what makes it exact.
     */
    private static void skipFully(DataInputStream data, int length)
            throws IOException {
        for (int skipped = 0; skipped < length;) {
            int step = data.skipBytes(length - skipped);
            if (step <= 0) {
                throw new IOException("truncated attribute");
            }
            skipped += step;
        }
    }
}
