/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import japicmp.model.JApiCompatibilityChangeType;

/**
 * Prints the semantic version level japicmp gives each type of compatibility
 * change, one {@code TYPE=LEVEL} line per type, for apiDiff.js. The XML report
 * of japicmp does not include the level.
 *
 * Run with: java -cp japicmp-jar-with-dependencies.jar
 * scripts/ChangeTypeLevels.java
 */
public class ChangeTypeLevels {

    public static void main(String[] args) {
        for (JApiCompatibilityChangeType type : JApiCompatibilityChangeType
                .values()) {
            System.out.println(
                    type.name() + "=" + type.getSemanticVersionLevel().name());
        }
    }
}
