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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;

import com.vaadin.flow.internal.FileIOUtils;

/**
 * Reads and writes the {@code overrides} block of a project's
 * {@code pnpm-workspace.yaml}, the location pnpm 10+ uses for dependency
 * overrides.
 * <p>
 * This file belongs to the user: it holds arbitrary pnpm settings, so
 * everything Flow does not manage has to survive an edit unchanged. That
 * includes the comments and layout a data-only representation throws away,
 * which is why the YAML nodes are edited through the snakeyaml-engine node API
 * rather than round-tripped through a databind layer. Reverting to a databind
 * round-trip destroys every comment in the file and reformats the rest of the
 * user's configuration, which is what issue #25122 was about.
 * <p>
 * Individual entries are merged in place, so entries that keep their version
 * are written back as the very nodes they were parsed from, and only the
 * entries Flow adds, changes or removes differ afterwards. The document is
 * still re-emitted rather than patched as text, so a few whole-file properties
 * are normalised: indentation becomes uniform, taken from what the file mostly
 * uses, and with it the indentation of block scalar content, which does not
 * change the value; a leading byte order mark and explicit {@code ---} or
 * {@code %YAML} markers are dropped; and the spacing inside flow collections is
 * rewritten.
 * <p>
 * Shapes that cannot be edited without risking the user's content are left
 * alone with a warning naming the cause: losing version locking is preferable
 * to corrupting the file. Those are an aliased overrides block, whose nodes are
 * shared with whatever else refers to it, a document that is not a single
 * mapping, and a document whose comments the parser cannot read, as rewriting
 * it would drop them. See {@link #canPersist()} for what callers have to do
 * about it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class PnpmWorkspaceFile {

    static final String WORKSPACE_FILE = "pnpm-workspace.yaml";
    private static final String OVERRIDES = "overrides";

    private static final LoadSettings LOAD_SETTINGS = LoadSettings.builder()
            .setParseComments(true).build();

    private static final LoadSettings LOAD_SETTINGS_WITHOUT_COMMENTS = LoadSettings
            .builder().setParseComments(false).build();

    /**
     * Indentation the emitter honours, used to clamp what is detected in the
     * file so that unusual indentation cannot fail the build. Anything above
     * this is rejected or silently ignored by the emitter.
     */
    private static final int MIN_INDENT = 2;
    private static final int MAX_INDENT = 9;

    private final File file;

    /**
     * Content as found on disk, or {@code null} when there is no file. Kept to
     * write the document back in the layout it already uses.
     */
    private final String originalContent;

    /**
     * Root mapping of the parsed document, or {@code null} when the document
     * holds content that cannot be edited safely.
     */
    private final MappingNode root;

    private final Map<String, String> loadedOverrides;

    private Map<String, String> overrides;

    /**
     * Why the file cannot be edited, for the warning that says so, or
     * {@code null} while it can be.
     */
    private String unsupportedReason;

    PnpmWorkspaceFile(File projectRoot) throws IOException {
        this.file = new File(projectRoot, WORKSPACE_FILE);
        this.originalContent = file.isFile()
                ? Files.readString(file.toPath(), StandardCharsets.UTF_8)
                : null;
        this.root = parse();
        this.loadedOverrides = readOverrides();
        this.overrides = loadedOverrides;
    }

    /**
     * Parses the document into an editable root mapping, returning {@code null}
     * when it holds content Flow must not rewrite.
     */
    private MappingNode parse() {
        if (originalContent == null || originalContent.isBlank()) {
            return emptyMapping();
        }
        Optional<Node> composed = compose(LOAD_SETTINGS);
        if (composed == null) {
            // Reading the comments is what fails for some documents that are
            // otherwise valid, such as one with a comment inside a flow
            // collection. Parsing again without them says which of the two it
            // is, so that the warning can name a cause the user can act on. The
            // file is left alone either way: writing it back without the
            // comments that could not be read would throw away exactly what
            // this class exists to keep.
            unsupportedReason = compose(LOAD_SETTINGS_WITHOUT_COMMENTS) != null
                    ? "its comments cannot be read, which a comment inside a "
                            + "flow collection, written with [] or {}, causes"
                    : "it does not hold a single YAML mapping, which a stream "
                            + "of several documents separated by '---' causes";
            return null;
        }
        if (composed.isEmpty()) {
            return emptyMapping();
        }
        if (!(composed.get() instanceof MappingNode mapping)) {
            return null;
        }
        // A document holding nothing but comments composes into a mapping
        // without entries, which the emitter cannot write back out. There is no
        // structure to preserve in that case, so start from an empty mapping.
        if (mapping.getValue().isEmpty()) {
            return emptyMapping();
        }
        return isEditable(mapping) ? mapping : null;
    }

    /**
     * Composes the document with the given settings, returning {@code null}
     * when it cannot be read at all, for instance a stream of several documents
     * separated by {@code ---}.
     */
    private Optional<Node> compose(LoadSettings settings) {
        try {
            return new Compose(settings).composeString(originalContent);
        } catch (RuntimeException e) {
            log().debug("Failed to parse {}", file, e);
            return null;
        }
    }

    /**
     * Tells whether the overrides of a document can be replaced in place. An
     * anchored block is shared with everything that refers to it, so editing
     * its entries would silently change those other keys as well, and a block
     * that is not a mapping at all is configuration Flow does not understand.
     */
    private static boolean isEditable(MappingNode mapping) {
        Optional<Node> block = overridesEntry(mapping)
                .map(index -> mapping.getValue().get(index).getValueNode());
        if (block.isEmpty()) {
            return true;
        }
        if (block.get().getAnchor().isPresent()) {
            return false;
        }
        return block.get() instanceof MappingNode || isNull(block.get());
    }

    /**
     * A key written without a value, as in a bare {@code overrides:} line.
     */
    private static boolean isNull(Node node) {
        return node instanceof ScalarNode scalar
                && Tag.NULL.equals(scalar.getTag());
    }

    private static MappingNode emptyMapping() {
        return new MappingNode(Tag.MAP, new ArrayList<>(), FlowStyle.BLOCK);
    }

    /**
     * Returns the position of the {@code overrides} entry in the given mapping.
     */
    private static Optional<Integer> overridesEntry(MappingNode mapping) {
        List<NodeTuple> entries = mapping.getValue();
        for (int index = 0; index < entries.size(); index++) {
            if (OVERRIDES.equals(scalarKey(entries.get(index)).orElse(null))) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    private Map<String, String> readOverrides() {
        return versionsOf(
                overridesBlock().map(MappingNode::getValue).orElse(List.of()));
    }

    /**
     * The versions the given entries hold, as reading the file back would
     * report them. Entries Flow does not manage have no version to report, so
     * they are left out, which is what makes writing and reading agree on them.
     */
    private static Map<String, String> versionsOf(List<NodeTuple> entries) {
        Map<String, String> versions = new LinkedHashMap<>();
        for (NodeTuple entry : entries) {
            managedKey(entry).ifPresent(key -> versions.put(key,
                    ((ScalarNode) entry.getValueNode()).getValue()));
        }
        return versions;
    }

    private Optional<MappingNode> overridesBlock() {
        if (root == null) {
            return Optional.empty();
        }
        return overridesEntry(root)
                .map(index -> root.getValue().get(index).getValueNode())
                .filter(MappingNode.class::isInstance)
                .map(MappingNode.class::cast);
    }

    private static Optional<String> scalarKey(NodeTuple entry) {
        return entry.getKeyNode() instanceof ScalarNode key
                ? Optional.of(key.getValue())
                : Optional.empty();
    }

    /**
     * Returns the key of an entry Flow manages: a plain {@code key: version}
     * pair. Anything else, such as a merge key or a nested mapping, is content
     * Flow does not know how to handle and leaves as it is.
     */
    private static Optional<String> managedKey(NodeTuple entry) {
        return entry.getValueNode() instanceof ScalarNode
                && !isNull(entry.getValueNode()) ? scalarKey(entry)
                        : Optional.empty();
    }

    /**
     * Returns the overrides the file held when it was read, as a flat
     * {@code key -> version} map. Writing through {@link #setOverrides(Map)}
     * does not change what this returns; read a new instance for that.
     */
    Map<String, String> getOverrides() {
        return new LinkedHashMap<>(loadedOverrides);
    }

    /**
     * Tells whether {@link #save()} is able to write the overrides at all. It
     * is {@code false} for a file holding content that cannot be edited without
     * risking the user's content, in which case the overrides Flow computed can
     * never be stored. Callers have to treat that as "nothing to do" rather
     * than as a pending change, as a pending change would have them redo the
     * work on every build.
     */
    boolean canPersist() {
        return root != null;
    }

    /**
     * Replaces the overrides block with the given entries, removing the block
     * entirely when the map is empty.
     */
    void setOverrides(Map<String, String> overrides) {
        this.overrides = new LinkedHashMap<>(overrides);
    }

    /**
     * Persists the file when its content changed. When the whole document is
     * empty the file is deleted, because an empty {@code pnpm-workspace.yaml}
     * carries no configuration; user-authored override keys and other sections
     * keep the document non-empty and thus keep the file alive.
     *
     * @return {@code true} if the file was written or deleted
     */
    boolean save() throws IOException {
        if (!canPersist()) {
            log().warn(
                    """
                            Cannot write dependency overrides to {}, as {}. The file is left \
                            as it is rather than rewritten, so platform versions are not \
                            locked for transitive dependencies. Changing the file as \
                            described, or maintaining the overrides manually, locks them \
                            again.""",
                    file, unsupportedReason);
            return false;
        }
        Set<String> unlockable = new LinkedHashSet<>();
        List<NodeTuple> merged = mergedEntries(unlockable);
        if (!unlockable.isEmpty()) {
            log().warn(
                    """
                            Not locking the version of {} in {}, as the file already has an \
                            entry for it that is not a plain version and that Flow leaves \
                            untouched. Removing that entry lets Flow manage the version \
                            again.""",
                    unlockable, file);
        }
        // What a later read of the file would report is what decides whether
        // there is anything to write, rather than the overrides that were asked
        // for. The two differ for an entry Flow cannot manage, which is left as
        // it is: asking for it again must not count as a pending change. Nor
        // must the same configuration in a different layout. Reporting either
        // as
        // a change marks package.json as modified, which runs a package install
        // on every dev-mode start.
        if (versionsOf(merged).equals(loadedOverrides)) {
            return false;
        }
        // The layout has to be read while the tree still only holds nodes that
        // came from the file, as the nodes Flow creates carry no position to
        // read it from.
        DumpSettings settings = dumpSettings();
        applyOverrides(merged);
        if (root.getValue().isEmpty()) {
            if (file.isFile()) {
                FileIOUtils.delete(file);
                return true;
            }
            return false;
        }
        Files.writeString(file.toPath(), dump(settings),
                StandardCharsets.UTF_8);
        return true;
    }

    /**
     * Merges the overrides into the document, entry by entry. Entries that keep
     * their version are left as the exact nodes they were parsed from, so their
     * comments, quoting and position survive; only changed values are replaced,
     * removed keys dropped and new keys appended.
     */
    private void applyOverrides(List<NodeTuple> merged) {
        Optional<Integer> position = overridesEntry(root);
        Optional<MappingNode> block = overridesBlock();
        if (merged.isEmpty()) {
            position.ifPresent(index -> updateRoot(
                    entries -> entries.remove(index.intValue())));
        } else if (position.isEmpty()) {
            updateRoot(entries -> entries
                    .add(new NodeTuple(overridesKey(), blockOf(merged))));
        } else if (block.isPresent()) {
            block.get().setValue(merged);
        } else {
            // A bare 'overrides:' line: keep its key, and with it any comment
            // attached to it, and give it the entries as its value.
            int index = position.get();
            updateRoot(entries -> entries.set(index, new NodeTuple(
                    entries.get(index).getKeyNode(), blockOf(merged))));
        }
    }

    private void updateRoot(Consumer<List<NodeTuple>> change) {
        List<NodeTuple> entries = new ArrayList<>(root.getValue());
        change.accept(entries);
        root.setValue(entries);
    }

    /**
     * @param unlockable
     *            collects the keys Flow was asked to override but holds back
     *            from, because the user wrote that entry in a shape Flow does
     *            not manage
     */
    private List<NodeTuple> mergedEntries(Set<String> unlockable) {
        List<NodeTuple> merged = new ArrayList<>();
        Set<String> present = new LinkedHashSet<>();
        for (NodeTuple entry : overridesBlock().map(MappingNode::getValue)
                .orElse(List.of())) {
            Optional<String> managed = managedKey(entry);
            if (managed.isEmpty()) {
                // Content Flow does not manage, kept as it is.
                merged.add(entry);
                scalarKey(entry).ifPresent(key -> {
                    // The key still counts as present, as appending it again
                    // would make the document a mapping with a duplicate key,
                    // which pnpm rejects.
                    present.add(key);
                    if (overrides.containsKey(key)) {
                        unlockable.add(key);
                    }
                });
                continue;
            }
            String key = managed.get();
            if (!overrides.containsKey(key)) {
                continue;
            }
            present.add(key);
            String version = overrides.get(key);
            ScalarNode value = (ScalarNode) entry.getValueNode();
            merged.add(version.equals(value.getValue()) ? entry
                    : new NodeTuple(entry.getKeyNode(),
                            replacementFor(value, version)));
        }
        overrides.forEach((key, version) -> {
            if (!present.contains(key)) {
                merged.add(new NodeTuple(scalar(key), scalar(version)));
            }
        });
        return merged;
    }

    /**
     * Builds the node for a version that changed, carrying over the comments
     * written alongside the old value so that a note on why a version is pinned
     * survives the version being updated.
     */
    private static ScalarNode replacementFor(ScalarNode previous,
            String version) {
        ScalarNode replacement = scalar(version);
        if (previous.getAnchor().isPresent()) {
            // Everything aliasing the old value still carries its comments, so
            // copying them here would print them twice.
            return replacement;
        }
        replacement.setBlockComments(previous.getBlockComments());
        replacement.setInLineComments(previous.getInLineComments());
        replacement.setEndComments(previous.getEndComments());
        return replacement;
    }

    private static MappingNode blockOf(List<NodeTuple> entries) {
        return new MappingNode(Tag.MAP, entries, FlowStyle.BLOCK);
    }

    /**
     * Wraps an override key or version Flow writes in double quotes. Package
     * names starting with {@code @} have to be quoted to be valid YAML, and
     * versions have to be quoted to stay strings instead of being read back as
     * numbers.
     */
    private static ScalarNode scalar(String value) {
        return new ScalarNode(Tag.STR, value, ScalarStyle.DOUBLE_QUOTED);
    }

    /**
     * The {@code overrides} key itself needs no quoting, unlike the package
     * names it holds.
     */
    private static ScalarNode overridesKey() {
        return new ScalarNode(Tag.STR, OVERRIDES, ScalarStyle.PLAIN);
    }

    private String dump(DumpSettings settings) {
        StringWriter writer = new StringWriter();
        new Dump(settings).dumpNode(root, new StreamDataWriter() {
            @Override
            public void write(String string) {
                writer.write(string);
            }

            @Override
            public void write(String string, int offset, int length) {
                writer.write(string, offset, length);
            }
        });
        return writer.toString();
    }

    /**
     * Emitter settings matching the layout the file already uses, so that
     * sections Flow did not touch come back out the way they went in. The
     * defaults match what pnpm itself writes. Indentation is read from the
     * parsed nodes rather than the text, so that indented prose inside a block
     * scalar is not mistaken for the document's own indentation.
     */
    private DumpSettings dumpSettings() {
        int indent = clamp(nestedIndent().orElse(MIN_INDENT));
        boolean indentedSequences = sequenceIndent()
                .map(sequenceIndent -> sequenceIndent > 0).orElse(true);
        return DumpSettings.builder().setDumpComments(true)
                .setDefaultFlowStyle(FlowStyle.BLOCK).setIndent(indent)
                .setIndentWithIndicator(indentedSequences)
                // The indicator counts towards the indent, so it has to stay
                // below it; the emitter rejects anything larger.
                .setIndicatorIndent(indentedSequences ? indent : 0)
                .setBestLineBreak(lineBreak())
                // Long entries stay on their own line rather than being folded.
                .setWidth(Integer.MAX_VALUE).build();
    }

    private static int clamp(int indent) {
        return Math.min(Math.max(indent, MIN_INDENT), MAX_INDENT);
    }

    /**
     * Columns the first nested mapping is indented by, relative to the key
     * holding it.
     */
    private Optional<Integer> nestedIndent() {
        for (NodeTuple entry : root.getValue()) {
            if (entry.getValueNode() instanceof MappingNode nested
                    && !nested.getValue().isEmpty()
                    && FlowStyle.BLOCK.equals(nested.getFlowStyle())) {
                Optional<Integer> nestedColumn = column(
                        nested.getValue().get(0).getKeyNode());
                Optional<Integer> keyColumn = column(entry.getKeyNode());
                if (nestedColumn.isPresent() && keyColumn.isPresent()) {
                    return Optional.of(nestedColumn.get() - keyColumn.get());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Columns the first block sequence's {@code -} indicator is indented by,
     * relative to the key holding it. Zero means sequences are written at their
     * key's own column.
     */
    private Optional<Integer> sequenceIndent() {
        for (NodeTuple entry : root.getValue()) {
            if (entry.getValueNode() instanceof SequenceNode sequence
                    && !sequence.getValue().isEmpty()
                    && FlowStyle.BLOCK.equals(sequence.getFlowStyle())) {
                Optional<Integer> itemColumn = column(
                        sequence.getValue().get(0));
                Optional<Integer> keyColumn = column(entry.getKeyNode());
                if (itemColumn.isPresent() && keyColumn.isPresent()) {
                    // The item starts after the '- ' indicator.
                    return Optional.of(itemColumn.get() - 2 - keyColumn.get());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> column(Node node) {
        return node.getStartMark().map(mark -> mark.getColumn());
    }

    /**
     * Keeps the line ending the file already uses, so that editing it does not
     * show up as a change to every line.
     */
    private String lineBreak() {
        return originalContent != null && originalContent.contains("\r\n")
                ? "\r\n"
                : "\n";
    }

    private static Logger log() {
        return LoggerFactory.getLogger(PnpmWorkspaceFile.class);
    }
}
