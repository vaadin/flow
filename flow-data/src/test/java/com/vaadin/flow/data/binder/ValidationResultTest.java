/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ValidationResultTest {

    @Test
    public void toStringValidation() {
        String toString = ValidationResult.ok().toString();
        assertEquals("ValidationResult{ok}", toString);

        toString = ValidationResult.error("My Error Message").toString();
        assertEquals(
                "ValidationResult{error='My Error Message', errorLevel=ERROR}",
                toString);

        toString = ValidationResult
                .create("My Critical Message", ErrorLevel.CRITICAL).toString();
        assertEquals(
                "ValidationResult{error='My Critical Message', errorLevel=CRITICAL}",
                toString);

        toString = ValidationResult.create("My Info Message", ErrorLevel.INFO)
                .toString();
        assertEquals(
                "ValidationResult{error='My Info Message', errorLevel=INFO}",
                toString);
    }

    @Test
    public void equalsAndHashCode() {
        ValidationResult ok1 = ValidationResult.ok();
        ValidationResult ok2 = ValidationResult.ok();
        ValidationResult error1 = ValidationResult.error("Msg1");
        ValidationResult error2 = ValidationResult.error("Msg2");
        ValidationResult info1 = ValidationResult.create("Info",
                ErrorLevel.INFO);
        ValidationResult info2 = ValidationResult.create("Info",
                ErrorLevel.ERROR);
        ValidationResult info3 = ValidationResult.create("Info",
                ErrorLevel.INFO);

        assertEquals(ok1, ok2);
        assertNotEquals(ok1, error1);
        assertNotEquals(error1, error2);
        assertNotEquals(error1, info1);
        assertNotEquals(info1, info2);
        assertEquals(info1, info3);
        assertEquals(ok1.hashCode(), ok2.hashCode());
        assertEquals(info1.hashCode(), info3.hashCode());
    }
}
