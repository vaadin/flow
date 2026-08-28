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
package com.vaadin.flow.devloop.test.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edits real source files and puts them back.
 * <p>
 * These ITs have to mutate sources, because that is the whole subject: a dev
 * loop is only testable by changing what is on disk. The rules that keep it
 * safe are that only files under a {@code mutable} package (or the stylesheet
 * and pom named by a test) are touched, and that every edit is reverted from
 * the original bytes - so a failed run, a Ctrl-C or a crashed daemon never
 * leaves the working tree dirty for the next one.
 */
final class SourcePatch implements AutoCloseable {

    /** The original bytes, keyed by file, in the order they were patched. */
    private final Map<Path, String> originals = new LinkedHashMap<>();

    /**
     * Replaces the first occurrence of a literal in a file.
     *
     * @param file
     *            the file to edit
     * @param find
     *            the literal to replace, which must occur
     * @param replace
     *            what to replace it with
     */
    void replace(Path file, String find, String replace) {
        String content = remember(file);
        int at = content.indexOf(find);
        if (at < 0) {
            throw new AssertionError(
                    "the fixture no longer contains \"" + find + "\": " + file);
        }
        // The first occurrence only. String.replace would take every one, and a
        // fixture edit that silently hits two places is a test that passes for
        // the wrong reason.
        write(file, content.substring(0, at) + replace
                + content.substring(at + find.length()));
    }

    /**
     * Adds text to the end of a file, which is the cheapest way to change its
     * bytes when what the file says does not matter - only that it changed.
     *
     * @param file
     *            the file to edit
     * @param text
     *            what to add
     */
    void append(Path file, String text) {
        write(file, remember(file) + text);
    }

    /**
     * Inserts text before the last closing brace of a Java source, which is how
     * a member is added to a class.
     *
     * @param file
     *            the source file
     * @param member
     *            the member declaration to add
     */
    void addMember(Path file, String member) {
        String content = remember(file);
        int at = content.lastIndexOf('}');
        if (at < 0) {
            throw new AssertionError("not a Java source: " + file);
        }
        write(file, content.substring(0, at) + member + "\n"
                + content.substring(at));
    }

    private String remember(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            originals.putIfAbsent(file, content);
            return content;
        } catch (IOException e) {
            throw new AssertionError("could not read " + file, e);
        }
    }

    private void write(Path file, String content) {
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("could not write " + file, e);
        }
    }

    /**
     * Puts every patched file back. Reports every failure rather than the
     * first, because a half-reverted tree is worse than a clear list of what to
     * fix.
     */
    @Override
    public void close() {
        List<String> failures = new ArrayList<>();
        originals.forEach((file, content) -> {
            try {
                Files.writeString(file, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                failures.add(file + ": " + e);
            }
        });
        originals.clear();
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "could not revert " + String.join(", ", failures));
        }
    }
}
