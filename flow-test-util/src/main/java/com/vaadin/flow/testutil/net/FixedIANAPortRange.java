// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.vaadin.flow.testutil.net;

/**
 * Fixed port range for when a defined can not be calculated.
 * <p>
 * Derived from SeleniumHQ / selenium
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class FixedIANAPortRange implements EphemeralPortRangeDetector {
    @Override
    public int getLowestEphemeralPort() {
        return 49152;
    }

    @Override
    public int getHighestEphemeralPort() {
        return 65535;
    }
}
