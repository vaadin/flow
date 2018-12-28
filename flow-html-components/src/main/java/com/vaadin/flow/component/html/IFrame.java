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
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Component representing a <code>&lt;iframe&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.3
 */
@Tag(Tag.IFRAME)
public class IFrame extends HtmlComponent {

  private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors
          .attributeWithDefault("src", "");

  private static final PropertyDescriptor<String, Optional<String>> srcdocDescriptor = PropertyDescriptors
          .optionalAttributeWithDefault("srcdoc", "");

  private static final PropertyDescriptor<String, Optional<String>> nameDescriptor = PropertyDescriptors
          .optionalAttributeWithDefault("name", "");

  private static final PropertyDescriptor<String, Optional<String>> sandboxDescriptor = PropertyDescriptors
          .optionalAttributeWithDefault("sandbox", "");

  private static final PropertyDescriptor<String, Optional<String>> allowDescriptor = PropertyDescriptors
          .optionalAttributeWithDefault("allow", "");

  private static final PropertyDescriptor<String, Optional<String>> importanceDescriptor = PropertyDescriptors
          .optionalAttributeWithDefault("importance", "");

  /**
   * Importance types.
   */
  public enum ImportanceType {
    AUTO("auto"),
    HIGH("high"),
    LOW("low");
    private final String value;

    ImportanceType(String value){
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    private static Optional<ImportanceType> fromAttributeValue(String value) {
      return Stream.of(values()).filter(type-> type.value.equals(value))
              .findFirst();
    }
  }

  /**
   * Sandbox types.
   */
  public enum SandboxType {
    RESTRICT_ALL(""),
    ALLOW_FORMS("allow-forms"),
    ALLOW_MODALS("allow-modals"),
    ALLOW_ORIENTATION_LOCK("allow-orientation-lock"),
    ALLOW_POINTER_LOCK("allow-pointer-lock"),
    ALLOW_POPUPS("allow-popups"),
    ALLOW_POPUPS_TO_ESCAPE_SANDBOX("allow-popups-to-escape-sandbox"),
    ALLOW_PRESENTATION("allow-presentation"),
    ALLOW_SAME_ORIGIN("allow-same-origin"),
    ALLOW_SCRIPTS("allow-scripts"),
    ALLOW_STORAGE_ACCESS_BY_USER_ACTIVATION("allow-storage-access-by-user-activation"),
    ALLOW_TOP_NAVIGATION("allow-top-navigation"),
    ALLOW_TOP_NAVIGATION_BY_USER_ACTIVATION("allow-top-navigation-by-user-activation");

    private final String value;

    SandboxType(String value){
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    private static Optional<SandboxType> fromAttributeValue(String value) {
      return Stream.of(values()).filter(type-> type.value.equals(value))
          .findFirst();
    }

  }

  /**
   * Creates a new iframe.
   */
  public IFrame() { }

  /**
   * Creates a new iframe with a source URL.
   * @param src Source URL
   */
  public IFrame(String src) {
    setSrc(src);
  }

  /**
   * Sets the source of the iframe.
   * @param src Source URL.
   */
  public void setSrc(String src) { set(srcDescriptor, src); }

  /**
   * @return the source of the iframe.
   */
  public String getSrc(){
    return get(srcDescriptor);
  }

  /**
   * Sets the srcdoc of the iframe.
   * @param srcdoc srcdoc URL.
   */
  public void setSrcdoc(String srcdoc){
    set(srcdocDescriptor, srcdoc);
  }

  /**
   * @return the srcdoc of the iframe.
   */
  public Optional<String> getSrcdoc() {
    return get(srcdocDescriptor);
  }

  /**
   * Sets the allow property to specify a feature policy. See <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Feature_Policy">Feature Policy</a>.
   *
   * @param allow Allow.
   */
  public void setAllow(String allow){
    set(allowDescriptor, allow);
  }

  /**
   * @return the currently applied allow value.
   */
  public Optional<String> getAllow() { return get(allowDescriptor); }

  /**
   * Sets the name attribute.
   * @param name name.
   */
  public void setName(String name) { set(nameDescriptor, name); }

  /**
   * @return the name attribute.
   */
  public Optional<String> getName() { return get(nameDescriptor); }

  /**
   * Sets the importance attribute to the specified {@link ImportanceType} value.
   * @param importance {@link ImportanceType} value.
   */
  public void setImportance(ImportanceType importance) { set(importanceDescriptor, importance.value); }

  /**
   * @return the importance value.
   */
  public Optional<ImportanceType> getImportance() { return get(importanceDescriptor).flatMap(ImportanceType::fromAttributeValue); }

  /**
   * Sets the sandbox attribute to the given {@link SandboxType}s.
   * @param types {@link SandboxType}s.
   */
  public void setSandbox(SandboxType ...types) {
    set(sandboxDescriptor, Stream.of(types).map(SandboxType::getValue).collect(Collectors.joining(" ")));
  }

  /**
   * @return the current {@link SandboxType}s.
   */
  public Optional<SandboxType[]> getSandbox() {
    return get(sandboxDescriptor)
        .map(value -> Stream.of(value.split(" "))
            .map(SandboxType::fromAttributeValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList()))
            .map(collection -> collection.toArray(new SandboxType[collection.size()]));
  }

}
