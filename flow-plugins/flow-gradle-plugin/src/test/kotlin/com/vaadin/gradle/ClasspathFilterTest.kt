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

import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
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
}
