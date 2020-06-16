/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Enum of supported units in Css sizes.
 */
public enum Unit {
    /**
     * Unit code representing pixels.
     */
    PIXELS("px"),
    /**
     * Unit code representing points (1/72nd of an inch).
     */
    POINTS("pt"),
    /**
     * Unit code representing picas (12 points).
     */
    PICAS("pc"),
    /**
     * Unit code representing the font-size of the root font.
     */
    REM("rem"),
    /**
     * Unit code representing the font-size of the relevant font.
     */
    EM("em"),
    /**
     * Unit code representing the x-height of the relevant font.
     */
    EX("ex"),
    /**
     * Unit code representing millimeters.
     */
    MM("mm"),
    /**
     * Unit code representing centimeters.
     */
    CM("cm"),
    /**
     * Unit code representing inches.
     */
    INCH("in"),
    /**
     * Unit code representing in percentage of the containing element
     * defined by terminal.
     */
    PERCENTAGE("%");

    private final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }

    static Stream<Unit> getUnits() {
        return Stream.of(Unit.values());
    }

    /**
     * Gives size unit of the css string representing a size.
     * 
     * @param cssSize Css compliant size string such as "50px".
     * 
     * @return A Optional unit.
     */
    public static Optional<Unit> getUnit(String cssSize) {
        if (cssSize == null) {
             throw new IllegalArgumentException("The parameter can't be null");
        }
        Stream<Unit> units = getUnits().filter(unit -> cssSize.endsWith(unit.toString()));
        return units.findFirst();
    }

    /**
     * Gives size component as float of the css string representing a size.
     * 
     * @param cssSize Css compliant size string such as "50px".
     * 
     * @return Size as float, 0 if string contained only the unit.
     */
    public static float getSize(String cssSize) {
        if (cssSize == null || cssSize.length() < 1) {
            throw new IllegalArgumentException("The parameter can't be null");
        }
        Unit unit = getUnits()
                .filter(value -> cssSize.endsWith(value.toString())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "The parameter string '%s' does not contain valid unit",
                        cssSize)));
        String size = cssSize
                .substring(0, cssSize.length() - unit.toString().length());
        if (size.isEmpty()) {
            size = "0";
        }
        return Float.valueOf(size);
    }

    /**
     * Convert unit string symbol to Unit.
     *
     * @param symbol A String.
     * @return A Unit, Unit.PIXELS if symbol was null or not matching.
     */
    public static Unit getUnitFromSymbol(String symbol) {
        if (symbol == null) {
            return Unit.PIXELS; // Defaults to pixels
        }
        for (Unit unit : Unit.values()) {
            if (symbol.equals(unit.getSymbol())) {
                return unit;
            }
        }
        return Unit.PIXELS; // Defaults to pixels
    }
}
