/**
 *    Copyright 2000-2026 Vaadin Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.flow.gradle

import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.specs.Spec
import java.io.Serializable
import java.util.function.Predicate

public data class ClasspathFilter(
    public val include: MutableList<String> = mutableListOf(),
    public val exclude: MutableList<String> = mutableListOf()
) : Serializable {
    public fun include(include: String) {
        this.include.add(include)
    }

    public fun exclude(exclude: String) {
        this.exclude.add(exclude)
    }

    public fun toPredicate(): Predicate<ModuleIdentifier> =
        toModuleIdentifierFilter()

    internal fun toModuleIdentifierFilter(): ModuleIdentifierFilter =
        ModuleIdentifierFilter(include.toList(), exclude.toList())
}

/**
 * Accepts the modules selected by a [ClasspathFilter]: a module is accepted
 * when it matches one of the [include] globs (or when no include is
 * configured) and matches none of the [exclude] globs. `com.vaadin:flow-server`
 * is always accepted, as the frontend scanner cannot work without it.
 *
 * Deliberately a plain serializable object holding nothing but the configured
 * globs, instead of a chain built with the `Predicate.and()`/`or()`/`negate()`
 * combinators. Those combinators return lambdas whose implementation class is
 * hosted in `java.base/java.util.function`, and Gradle cannot store such a
 * lambda in a configuration cache entry: a project with a file-based
 * dependency (`implementation files(...)`) keeps the artifact view's component
 * filter - and with it this predicate - alive in the serialized task graph
 * instead of flattening it away, and the build then fails with
 * `module java.base does not "opens java.util.function"`. The globs are
 * compiled on demand for the same reason, so that no derived state reaches the
 * cache entry either.
 */
internal data class ModuleIdentifierFilter(
    private val include: List<String>,
    private val exclude: List<String>
) : Predicate<ModuleIdentifier>, Serializable {

    override fun test(t: ModuleIdentifier): Boolean = when {
        ModuleIdentifierPredicate.FLOW_SERVER.test(t) -> true
        exclude.any { matches(it, t) } -> false
        else -> include.isEmpty() || include.any { matches(it, t) }
    }

    private fun matches(glob: String, moduleIdentifier: ModuleIdentifier): Boolean =
        ModuleIdentifierPredicate.fromGroupNameGlob(glob).test(moduleIdentifier)
}

/**
 * Selects the components of a dependency configuration whose module is
 * accepted by [classpathFilter]. Components that are not
 * [ModuleComponentIdentifier]s - a local library, or another module of the
 * same build - are always accepted, as no module coordinates exist to match
 * them against.
 *
 * A named class rather than a lambda so that Gradle serializes it as an
 * ordinary bean: see [ModuleIdentifierFilter] for why a component filter can
 * end up in a configuration cache entry.
 */
internal class ClasspathComponentFilter(classpathFilter: ClasspathFilter) :
    Spec<ComponentIdentifier>, Serializable {

    // Snapshots the configured globs at construction time, as the predicate
    // chain it replaces used to. Typed as the filter implementation rather
    // than as Predicate, so that nothing unstorable - a lambda, or a chain
    // built from the Predicate combinators - can reach the configuration
    // cache entry through this field.
    private val artifactFilter: ModuleIdentifierFilter =
        classpathFilter.toModuleIdentifierFilter()

    override fun isSatisfiedBy(element: ComponentIdentifier): Boolean =
        element !is ModuleComponentIdentifier ||
                artifactFilter.test(element.moduleIdentifier)
}

/**
 * Matches strings based on given [pattern].
 * @property pattern a pattern such as `com.vaadin` or `*`.
 */
public data class GlobMatcher(public val pattern: String) : Predicate<String> {
    private val matcher = pattern.replace(".", "\\.").replace("*", ".*").toRegex()
    override fun test(t: String): Boolean = matcher.matches(t)
}

/**
 * Matches [ModuleIdentifier]s.
 */
public data class ModuleIdentifierPredicate(
    private val groupMatcher: Predicate<String>,
    private val nameMatcher: Predicate<String>
) : Predicate<ModuleIdentifier> {
    override fun test(t: ModuleIdentifier): Boolean = groupMatcher.test(t.group) && nameMatcher.test(t.name)

    public companion object {
        /**
         * Creates a matcher matching [groupNameGlob].
         * @param groupNameGlob matches both groupId and artifactId. A glob-like
         * pattern separated by a colon, for example `com.vaadin:*`.
         */
        public fun fromGroupNameGlob(groupNameGlob: String): ModuleIdentifierPredicate {
            val patterns = groupNameGlob.split(':')
            require(patterns.size == 2) { "$groupNameGlob: Invalid format, expected two patterns separated by colon, for example com.vaadin:*" }
            return ModuleIdentifierPredicate(GlobMatcher(patterns[0]), GlobMatcher(patterns[1]))
        }

        public val FLOW_SERVER: ModuleIdentifierPredicate = fromGroupNameGlob("com.vaadin:flow-server")

        public val ANY: ModuleIdentifierPredicate = fromGroupNameGlob("*:*")
    }
}
