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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.junit.Test
import kotlin.test.expect

class ClasspathFilterTest {

    @Test
    fun testEmptyAcceptsAnything() {
        val m = ClasspathFilter().toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
    }

    @Test
    fun `flow-server cannot be excluded`() {
        val m = ClasspathFilter().apply {
            exclude("com.vaadin:flow-*")
        }.toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-something")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
    }

    @Test
    fun `multiple excludes`() {
        val m = ClasspathFilter().apply {
            exclude("com.vaadin:*")
            exclude("org.foo:*")
        }.toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
    }

    @Test
    fun `flow-server cannot be excluded by omission`() {
        val m = ClasspathFilter().apply {
            include("com.vaadin:checkbox")
        }.toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-something")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
    }

    @Test
    fun `multiple includes`() {
        val m = ClasspathFilter().apply {
            include("com.vaadin:*")
            include("org.foo:*")
        }.toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.foo", "bar")) }
    }

    @Test
    fun `include-exclude flow-server cannot be excluded by omission`() {
        val m = ClasspathFilter().apply {
            include("com.vaadin:checkbox")
            exclude("com.vaadin:flow-server")
        }.toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-something")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
    }

    @Test
    fun `exclude takes precedence`() {
        val m = ClasspathFilter().apply {
            include("com.vaadin:*")
            exclude("com.vaadin:checkbox")
        }.toPredicate()
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.foo", "bar")) }
    }

    /**
     * Regression test for the configuration cache failure that hits any
     * project with a file-based dependency (`implementation files(...)`).
     * Such a dependency makes Gradle keep the artifact view's component
     * filter - and with it this predicate - alive in the serialized task
     * graph instead of flattening it away. A predicate composed out of the
     * `Predicate.and()`/`or()`/`negate()` default methods is a lambda hosted
     * in `java.base/java.util.function`, which Gradle can neither serialize
     * nor reflect into ("module java.base does not opens java.util.function"),
     * so the build fails while storing the entry.
     *
     * The predicate must therefore be an ordinary serializable object.
     */
    @Test
    fun `default predicate is serializable`() {
        val m = serializeAndDeserialize(ClasspathFilter().toPredicate())
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
    }

    @Test
    fun `configured predicate is serializable`() {
        val m = serializeAndDeserialize(ClasspathFilter().apply {
            include("com.vaadin:*")
            exclude("com.vaadin:checkbox")
        }.toPredicate())
        expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        expect(false) { m.test(DefaultModuleIdentifier.newId("com.foo", "bar")) }
    }

    /**
     * The object Gradle actually stores in the configuration cache entry is
     * the component filter, not the predicate it delegates to, so it has to
     * survive a round trip as well.
     */
    @Test
    fun `component filter is serializable`() {
        val filter = serializeAndDeserialize(ClasspathComponentFilter(
            ClasspathFilter().apply {
                include("com.vaadin:*")
                exclude("com.vaadin:checkbox")
            }))
        expect(true) { filter.isSatisfiedBy(moduleComponentId("com.vaadin", "flow-server")) }
        expect(false) { filter.isSatisfiedBy(moduleComponentId("com.vaadin", "checkbox")) }
        expect(false) { filter.isSatisfiedBy(moduleComponentId("org.foo", "bar")) }
        // a component without module coordinates, e.g. a local jar
        expect(true) { filter.isSatisfiedBy(ComponentIdentifier { "local.jar" }) }
    }

    private fun moduleComponentId(group: String, name: String): ComponentIdentifier =
        DefaultModuleComponentIdentifier.newId(
            DefaultModuleIdentifier.newId(group, name), "1.0"
        )

    private fun <T> serializeAndDeserialize(value: T): T {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        @Suppress("UNCHECKED_CAST")
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray()))
            .use { it.readObject() } as T
    }
}
