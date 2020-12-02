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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

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
    return classFinder.getSubTypesOf(serviceClass).stream()
      .filter(ReflectTools::isInstantiable)
      .map(ReflectTools::createInstance)
      .collect(Collectors.toList());
  }
  
}
