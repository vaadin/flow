/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

@Alternative
@Dependent
@Priority(100)
class TestDeploymentValidator extends DeploymentValidator {

    @ApplicationScoped
    static class BeanInfoSetHolder {

        private Set<BeanInfo> infoSet;

        Set<BeanInfo> getInfoSet() {
            return Collections.unmodifiableSet(infoSet);
        }

        void setInfoSet(Set<BeanInfo> infoSet) {
            this.infoSet = infoSet;
        }

    }

    @Inject
    private BeanInfoSetHolder beanInfoSetHolder;

    @Override
    void validate(Set<BeanInfo> infoSet, Consumer<Throwable> problemConsumer) {
        // No-op.
        // We need CDI to startup in Unit tests even with
        // intentionally misconfigured beans.
        beanInfoSetHolder.setInfoSet(infoSet);
    }

    void validateForTest(Set<BeanInfo> infoSet,
            Consumer<Throwable> problemConsumer) {
        super.validate(infoSet, problemConsumer);
    }

}
