package com.vaadin.flow.server;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

public class HttpStatusCodeTest {

    @Test
    public void isValidStatusCode_invalidCode_returnsFalse() {
        Set<Integer> validCodes = Stream.of(HttpStatusCode.values())
                .map(HttpStatusCode::getCode).collect(Collectors.toSet());

        IntStream.rangeClosed(-1000, 1000)
                .filter(sc -> !validCodes.contains(sc))
                .forEach(sc -> Assert.assertFalse(
                        sc + " should be invalid, but was not",
                        HttpStatusCode.isValidStatusCode(sc)));
    }

    @Test
    public void isValidStatusCode_validCode_returnsTrue() {
        Stream.of(HttpStatusCode.values()).mapToInt(HttpStatusCode::getCode)
                .forEach(sc -> Assert.assertTrue(
                        sc + " should be valid, but was not",
                        HttpStatusCode.isValidStatusCode(sc)));

    }

}
