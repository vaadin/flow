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
package com.vaadin.flow.utils;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import org.slf4j.LoggerFactory;

public class LookupImpl implements Lookup {

  private ClassFinder classFinder;

  public LookupImpl(ClassFinder classFinder) {
    this.classFinder = classFinder;
  }

  @Override
  public <T> T lookup(Class<T> serviceClass) {
    return lookupAll(serviceClass).stream().findFirst().orElse(null);
  }

  @Override
  public <T> Collection<T> lookupAll(Class<T> serviceClass) {
    Collection<T> result = Collections.emptyList();
    classFinder.getSubTypesOf(serviceClass).forEach(clz -> {
      try {
        result.add(clz.newInstance());
      } catch (Exception e) {
        LoggerFactory.getLogger(LookupImpl.class.getName())
                    .warn("Failed to create an insance of type {}", clz);
      }
    });
    return result;
  }
  
}
