/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Identifier for a chunk or part of the JS bundle.
 * <p>
 * Each chunk can be loaded at a separate time. Chunks marked as eager are
 * loaded immediately when the JS bundle is loaded while chunks marked as not
 * eager (i.e. lazy) are loaded on demand later.
 * <p>
 * There is one special, global chunk, defined as {@link #GLOBAL} in this class,
 * which is used for gathering all data that relates to internal entry points.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 **/
public class ChunkInfo {

    public static final ChunkInfo GLOBAL = new ChunkInfo(
            EntryPointType.INTERNAL, null, null, false);

    private final EntryPointType type;
    private final String name;

    private List<String> dependencyTriggers = null;

    private final boolean eager;

    public ChunkInfo(EntryPointType type, String name,
            List<String> dependencyTriggers, boolean eager) {
        this.type = type;
        this.eager = eager;

        if (type == EntryPointType.INTERNAL) {
            this.name = null;
        } else {
            this.name = name;
            this.dependencyTriggers = dependencyTriggers;
        }
    }

    public EntryPointType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result
                + (dependencyTriggers != null ? dependencyTriggers.hashCode()
                        : 0);
        result = 31 * result + (eager ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ChunkInfo chunkInfo = (ChunkInfo) o;

        if (eager != chunkInfo.eager)
            return false;
        if (type != chunkInfo.type)
            return false;
        if (!Objects.equals(name, chunkInfo.name))
            return false;
        return Objects.equals(dependencyTriggers, chunkInfo.dependencyTriggers);
    }

    public List<String> getDependencyTriggers() {
        if (this.dependencyTriggers != null) {
            return this.dependencyTriggers;
        } else {
            return Collections.singletonList(getName());
        }
    }

    public boolean isEager() {
        return eager;
    }

}
