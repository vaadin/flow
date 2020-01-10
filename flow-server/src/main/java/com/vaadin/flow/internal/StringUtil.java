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
package com.vaadin.flow.internal;

import java.util.Scanner;

/**
 * Utility class for special string handling.
 */
public final class StringUtil {

    /**
     * Comment parser state enumeration.
     */
    private enum State {
        NORMAL, IN_LINE_COMMENT, IN_BLOCK_COMMENT, IN_STRING
    }

    /**
     * Removes comments (block comments and line comments) from the JS code.
     *
     * @return the code with removed comments
     */
    public final static String removeComments(String code) {
        State state = State.NORMAL;
        StringBuilder result = new StringBuilder();
        Scanner scanner = new Scanner(code);
        scanner.useDelimiter("");
        while (scanner.hasNext()) {
            String character = scanner.next();
            switch (state) {
            case NORMAL:
                if (character.equals("/") && scanner.hasNext()) {
                    String nextCharacter = scanner.next();
                    if (nextCharacter.equals("/")) {
                        state = State.IN_LINE_COMMENT;
                    } else if (nextCharacter.equals("*")) {
                        state = State.IN_BLOCK_COMMENT;
                    } else {
                        result.append(character).append(nextCharacter);
                    }
                } else {
                    result.append(character);
                    if (character.equals("\"")) {
                        state = State.IN_STRING;
                    }
                }
                break;
            case IN_STRING:
                result.append(character);
                if (character.equals("\"")) {
                    state = State.NORMAL;
                } else if (character.equals("\\") && scanner.hasNext()) {
                    result.append(scanner.next());
                }
                break;
            case IN_LINE_COMMENT:
                if (character.equals("\n")) {
                    result.append(character);
                    state = State.NORMAL;
                }
                break;
            case IN_BLOCK_COMMENT:
                if (character.equals("*") && scanner.hasNext() && scanner
                        .next().equals("/")) {
                    state = State.NORMAL;
                    break;
                }
            }
        }
        scanner.close();
        return result.toString();
    }
}
