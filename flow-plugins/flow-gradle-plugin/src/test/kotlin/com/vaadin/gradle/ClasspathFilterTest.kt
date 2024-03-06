/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

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
