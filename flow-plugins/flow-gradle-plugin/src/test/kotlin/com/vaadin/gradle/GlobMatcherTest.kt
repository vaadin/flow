package com.vaadin.flow.gradle

import org.junit.Test
import kotlin.test.expect

class GlobMatcherTest {
    @Test
    fun testMatchAnything() {
        val m = GlobMatcher("*")
        expect(true) { m.test("flow-server") }
        expect(true) { m.test("com.vaadin") }
    }

    @Test
    fun testEndsWith() {
        val m = GlobMatcher("com.*")
        expect(false) { m.test("flow-server") }
        expect(true) { m.test("com.vaadin") }
        expect(false) { m.test("org.foo") }
        expect(false) { m.test("comma.foo") }
    }

    @Test
    fun testString() {
        val m = GlobMatcher("flow-server")
        expect(true) { m.test("flow-server") }
        expect(false) { m.test("com.vaadin") }
        expect(false) { m.test("org.foo") }
    }
}