/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

class CommunicationUtil {

    static String getStringWhenWriteBytesOffsetLength(OutputStream outputStream)
            throws IOException {
        ArgumentCaptor<byte[]> contentArg = ArgumentCaptor
                .forClass(byte[].class);
        ArgumentCaptor<Integer> offsetArg = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(int.class);

        Mockito.verify(outputStream, VerificationModeFactory.atLeastOnce())
                .write(contentArg.capture(), offsetArg.capture(),
                        lengthArg.capture());

        List<Integer> offsetValues = offsetArg.getAllValues();
        List<Integer> lengthValues = lengthArg.getAllValues();

        AtomicInteger i = new AtomicInteger();

        return contentArg.getAllValues().stream().map(bytes -> {
            return new String(bytes, offsetValues.get(i.get()),
                    lengthValues.get(i.getAndIncrement()));
        }).collect(Collectors.joining());
    }

    static String getStringWhenWriteString(OutputStream outputStream)
            throws IOException {
        ArgumentCaptor<byte[]> contentArg = ArgumentCaptor
                .forClass(byte[].class);

        Mockito.verify(outputStream, VerificationModeFactory.atLeastOnce())
                .write(contentArg.capture());

        return contentArg.getAllValues().stream()
                .map(bytes -> new String(bytes)).collect(Collectors.joining());
    }

    static String getStringWhenWriteString(PrintWriter printWriter) {
        ArgumentCaptor<String> contentArg = ArgumentCaptor
                .forClass(String.class);

        Mockito.verify(printWriter, VerificationModeFactory.atLeastOnce())
                .write(contentArg.capture());

        return String.join("", contentArg.getAllValues());
    }

}
