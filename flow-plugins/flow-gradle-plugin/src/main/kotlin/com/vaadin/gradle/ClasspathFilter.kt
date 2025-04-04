/**
 *    Copyright 2000-2020 Vaadin Ltd
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

    public fun toPredicate(): Predicate<ModuleIdentifier> {
        val includeMatchers = include.map { ModuleIdentifierPredicate.fromGroupNameGlob(it) }
        val excludeMatchers = exclude.map { ModuleIdentifierPredicate.fromGroupNameGlob(it) }
        val excludeMatcher: Predicate<ModuleIdentifier> = excludeMatchers.or()
        val includeMatcher: Predicate<ModuleIdentifier> = if (includeMatchers.isEmpty()) {
            ModuleIdentifierPredicate.ANY
        } else {
            includeMatchers.or()
        }
        return includeMatcher.and(excludeMatcher.negate()).or(ModuleIdentifierPredicate.FLOW_SERVER)
    }
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

        public val ANY: ModuleIdentifierPredicate = ModuleIdentifierPredicate({ true }, { true })
    }
}

private fun <T> List<Predicate<T>>.or(): Predicate<T> = Predicate { probe ->
    any { predicate -> predicate.test(probe) }
}
