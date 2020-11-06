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
package com.vaadin.flow.osgi.support.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import com.vaadin.flow.osgi.support.OsgiVaadinContributor;
import com.vaadin.flow.osgi.support.OsgiVaadinStaticResource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith({ BundleContextExtension.class, ServiceExtension.class, })
public class VaadinResourceTrackerComponentTest {

    @InjectBundleContext
    BundleContext bc;
    @InjectService(timeout = 1000)
    HttpService httpService;
    @InjectService(timeout = 1000)
    HttpServiceRuntime httpServiceRuntime;

    static OsgiVaadinStaticResource vaadinStaticResourceOf(String path) {
        return new OsgiVaadinStaticResource() {

            @Override
            public String getPath() {
                return "/" + path;
            }

            @Override
            public String getAlias() {
                return "/alias/" + path;
            }
        };
    }

    @Nested
    class OsgiVaadinStaticResourceTest {

        private static final String FOO = "foo";
        private static final String ALIAS_FOO = "/alias/foo";

        @org.junit.jupiter.api.Test
        void registerService_exists()
                throws Exception, IllegalArgumentException {


            RequestInfoDTO requestInfoDTO = httpServiceRuntime
                    .calculateRequestInfoDTO(ALIAS_FOO);

            assertThat(requestInfoDTO.servletDTO).isNull();

            ServiceRegistration<OsgiVaadinStaticResource> serviceRegistration = bc
                    .registerService(OsgiVaadinStaticResource.class,
                            vaadinStaticResourceOf(FOO),
                            new Hashtable<String, Object>());

            requestInfoDTO = httpServiceRuntime
                    .calculateRequestInfoDTO(ALIAS_FOO);
            assertThat(requestInfoDTO.servletDTO).isNotNull();

            serviceRegistration.unregister();

            requestInfoDTO = httpServiceRuntime
                    .calculateRequestInfoDTO(ALIAS_FOO);

            assertThat(requestInfoDTO.servletDTO).isNull();

            System.out.println(1);
        }
    }

    @Nested
    class OsgiVaadinContributorTest {

        @org.junit.jupiter.api.Test
        void registerService_exists() throws Exception {

            // printServletContexts();
            RequestInfoDTO requestInfoDTO = httpServiceRuntime
                    .calculateRequestInfoDTO("/alias/foo");

            assertThat(requestInfoDTO.servletDTO).isNull();

            ServiceRegistration<OsgiVaadinContributor> serviceRegistration = bc
                    .registerService(OsgiVaadinContributor.class,
                            new OsgiVaadinContributor() {

                        @Override
                        public List<OsgiVaadinStaticResource> getContributions() {

                            return Collections.singletonList(
                                    vaadinStaticResourceOf("foo"));
                        }
                    }, new Hashtable<String, Object>());

            requestInfoDTO = httpServiceRuntime
                    .calculateRequestInfoDTO("/alias/foo");
            assertThat(requestInfoDTO.servletDTO).isNotNull();

            serviceRegistration.unregister();
            requestInfoDTO = httpServiceRuntime
                    .calculateRequestInfoDTO("/alias/foo");

            assertThat(requestInfoDTO.servletDTO).isNull();
        }
    }
}
