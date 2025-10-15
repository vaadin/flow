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
 * There is a special application shell chunk, defined as {@link #APP_SHELL}
 * in this class, which is used for gathering all data that relates to the
 * application shell.
 * <p>
 * There is a special global chunk, defined as {@link #GLOBAL} in this class,
 * which is used for gathering all data that relates to internal entry points.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 **/
public class ChunkInfo {

    public static final ChunkInfo APP_SHELL = new ChunkInfo(EntryPointType.INTERNAL, null
    , null, true);

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
