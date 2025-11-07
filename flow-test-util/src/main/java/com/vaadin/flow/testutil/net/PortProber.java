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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for determining a free localhost port that is not used by any ipv4 or
 * ipv6 interfaces.
 * <p>
 * Derived from SeleniumHQ / selenium
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class PortProber {

    private static InetAddress ipv4All;
    private static InetAddress ipv6All;
    static {
        try {
            ipv4All = InetAddress.getByName("0.0.0.0");
            ipv6All = InetAddress.getByName("::0");
        } catch (UnknownHostException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final Random random = new Random();
    private static final EphemeralPortRangeDetector ephemeralRangeDetector;
    
        enum OS {
            WINDOWS, MAC, LINUX, SUN_OS;

        /**
         * Use system property to figure out the operating system.
         *
         * @return current operating system
         */
        public static OS guess() {
            final String osName = System.getProperty("os.name");
            return osName.contains("Windows") ? OS.WINDOWS
                    : osName.contains("Mac") ? OS.MAC
                            : osName.contains("SunOS") ? OS.SUN_OS : OS.LINUX;
        }
    }


    static {
        
        if (OS.guess() == OS.LINUX) {
            ephemeralRangeDetector = LinuxEphemeralPortRangeDetector
                    .getInstance();
        } else {
            ephemeralRangeDetector = new FixedIANAPortRange();
        }
    }

    public static final int HIGHEST_PORT = 65535;
    public static final int START_OF_USER_PORTS = 1024;

    private PortProber() {
        // Utility class
    }

    /**
     * Get a free localhost port that is not in use by any interface.
     *
     * @return found free port
     * @throws RuntimeException
     *             if no free port was found
     */
    public static int findFreePort() {
        for (int i = 0; i < 10; i++) {
            int seedPort = createAcceptablePort();
            int suggestedPort = checkPortIsFree(seedPort);
            if (suggestedPort != -1) {
                return suggestedPort;
            }
        }
        throw new RuntimeException("Unable to find a free port");
    }

    /**
     * Returns a random port within the systems ephemeral port range
     * <p/>
     * See https://en.wikipedia.org/wiki/Ephemeral_ports for more information.
     * <p/>
     * If the system provides a too short range (mostly on old windows systems)
     * the port range suggested from Internet Assigned Numbers Authority will be
     * used.
     *
     * @return a random port number
     */
    private static int createAcceptablePort() {
        synchronized (random) {
            int FIRST_PORT = Math.max(START_OF_USER_PORTS,
                    ephemeralRangeDetector.getLowestEphemeralPort());
            int LAST_PORT = Math.min(HIGHEST_PORT,
                    ephemeralRangeDetector.getHighestEphemeralPort());

            if (LAST_PORT - FIRST_PORT < 5000) {
                EphemeralPortRangeDetector ianaRange = new FixedIANAPortRange();
                FIRST_PORT = ianaRange.getLowestEphemeralPort();
                LAST_PORT = ianaRange.getHighestEphemeralPort();
            }

            if (FIRST_PORT == LAST_PORT) {
                return FIRST_PORT;
            }
            if (FIRST_PORT > LAST_PORT) {
                throw new UnsupportedOperationException(
                        "Could not find ephemeral port to use");
            }
            final int randomInt = random.nextInt();
            final int portWithoutOffset = Math
                    .abs(randomInt % (LAST_PORT - FIRST_PORT + 1));
            return portWithoutOffset + FIRST_PORT;
        }
    }

    private static int checkPortIsFree(int port) {
        int validPort = -1;
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("localhost", port));

            validPort = socket.getLocalPort();
        } catch (IOException e) {
            // Can not bind port as it is in use
            getLogger().trace("Port '{}' in use for localhost.", port);
            return -1;
        }
        try (ServerSocket ipv4socket = new ServerSocket()) {
            ipv4socket.bind(new InetSocketAddress(ipv4All, port));
        } catch (IOException e) {
            // Can not bind port as it is in use in an ipv4 interface
            getLogger().trace("Port '{}' in use for ipv4 interface.", port);
            return -1;
        }
        try (ServerSocket ipv6socket = new ServerSocket()) {
            ipv6socket.bind(new InetSocketAddress(ipv6All, port));
        } catch (IOException e) {
            // Can not bind port as it is in use in an ipv6 interface
            getLogger().trace("Port '{}' in use for ipv6 interface.", port);
            return -1;
        }
        return validPort;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(PortProber.class);
    }
}
