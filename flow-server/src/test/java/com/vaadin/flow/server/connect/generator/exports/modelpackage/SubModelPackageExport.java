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
 */
package com.vaadin.flow.server.connect.generator.exports.modelpackage;

import com.vaadin.flow.server.connect.Export;
import com.vaadin.flow.server.connect.generator.exports.modelpackage.subpackage.Account;

@Export
public class SubModelPackageExport {

    public Account getSubAccountPackage(String name) {
        return new Account();
    }

}
