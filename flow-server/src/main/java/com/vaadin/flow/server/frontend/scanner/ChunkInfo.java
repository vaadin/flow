/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.router.Load;

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
 **/
public class ChunkInfo {

    public static final ChunkInfo GLOBAL = new ChunkInfo(
            EntryPointType.INTERNAL, null, Load.ON_STARTUP, null);

    private final EntryPointType type;
    private final String name;
    private final Load dependencies;

    private List<String> dependencyTriggers = null;

    public ChunkInfo(EntryPointType type, String name, Load dependencies,
            List<String> dependencyTriggers) {
        this.type = type;
        if (type == EntryPointType.INTERNAL) {
            this.name = null;
            this.dependencies = Load.ON_STARTUP;
        } else {
            this.name = name;
            this.dependencies = dependencies;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChunkInfo other = (ChunkInfo) obj;
        if (type != other.type) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public Load getDependencies() {
        return dependencies;
    }

    public List<String> getDependencyTriggers() {
        if (this.dependencyTriggers != null) {
            return this.dependencyTriggers;
        } else {
            return Collections.singletonList(getName());
        }
    }

}
