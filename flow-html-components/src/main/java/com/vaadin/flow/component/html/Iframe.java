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

import com.vaadin.flow.component.Component;
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
public class Iframe extends Component {

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
   * @param src Source URL
   */
  public Iframe(String src) {
    setSrc(src);
  }

  /**
   * Sets the allow property to specify a feature policy. See <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Feature_Policy">Feature Policy</a>.
   *
   * @param allow Allow.
   */
  public void setAllow(String allow){
    getElement().setAttribute("allow", allow);
  }

  /**
   * @return the currently applied allow value.
   */
  public Optional<String> getAllow(){
    return optionallyGetAttribute("allow");
  }

  /**
   * Sets the height in CSS pixels.
   * @param height Pixels.
   */
  public void setHeight(Integer height){
    getElement().setAttribute("height", height.toString());
  }

  /**
   * @return the current height attribute.
   */
  public Optional<Integer> getHeight(){
    return optionallyGetAttribute("height")
        .map(Integer::valueOf);
  }


  /**
   * Sets the importance attribute to the specified {@link ImportanceType} value.
   * @param importance {@link ImportanceType} value.
   */
  public void setImportance(ImportanceType importance){
    getElement().setAttribute("value", importance.value);
  }

  /**
   * @return the importance value.
   */
  public Optional<ImportanceType> getImportance(){
    return optionallyGetAttribute("importance")
        .flatMap(ImportanceType::fromAttributeValue);
  }

  /**
   * Sets the name attribute.
   * @param name name.
   */
  public void setName(String name){
    getElement().setAttribute("name", name);
  }

  /**
   * @return the name attribute.
   */
  public Optional<String> getName() {
    return optionallyGetAttribute("name");
  }

  /**
   * Sets the sandbox attribute to the given {@link SandboxType}s.
   * @param types {@link SandboxType}s.
   */
  public void setSandbox(SandboxType ...types) {
    getElement().setAttribute("sandbox", Stream.of(types).map(SandboxType::getValue).collect(Collectors.joining(" ")));
  }

  /**
   * @return the current {@link SandboxType}s.
   */
  public Optional<Collection<SandboxType>> getSandbox(){
    return optionallyGetAttribute("sandbox")
        .map(value -> Stream.of(value.split(" "))
            .map(SandboxType::fromAttributeValue)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList()));
  }

  /**
   * Sets the source of the iframe.
   * @param src Source URL.
   */
  public void setSrc(String src) {
    getElement().setAttribute("src", src);
  }

  /**
   * @return the source of the iframe.
   */
  public String getSrc(){
    return getElement().getAttribute("src");
  }

  /**
   * Sets the srcdoc of the iframe.
   * @param srcdoc srcdoc URL.
   */
  public void setSrcdoc(String srcdoc){
    getElement().setAttribute("srcdoc", srcdoc);
  }

  /**
   * @return the srcdoc of the iframe.
   */
  public Optional<String> getSrcdoc() {
    return optionallyGetAttribute("srcdoc");
  }

  /**
   * Sets the width of the iframe.
   * @param width Width in CSS pixels.
   */
  public void setWidth(Integer width) {
    getElement().setAttribute("width", width.toString());
  }

  /**
   * @return the width of the iframe.
   */
  public Optional<Integer> getWidth() {
    return optionallyGetAttribute("width")
        .map(Integer::valueOf);
  }


  /**
   * Tries to get an attribute value.
   * @param attributeName Name of attribute
   * @return Optional containing value, or empty optional
   */
  private Optional<String> optionallyGetAttribute(String attributeName){
    String value = getElement().getAttribute(attributeName);

    if(value!=null){
      return Optional.of(value);
    } else {
      return Optional.empty();
    }
  }

}
