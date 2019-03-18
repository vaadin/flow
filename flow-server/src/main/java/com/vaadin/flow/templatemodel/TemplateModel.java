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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a template model. Extending this interface and adding getters and
 * setters makes it possible to easily bind data to a template.
 * <p>
 * It is also possible to import a Bean's properties to the model using
 * {@link #importBean(String, Object, Predicate)}
 * <p>
 * <b>Supported property types</b>:
 * <ul>
 * <li>boolean &amp; Boolean</li>
 * <li>int &amp; Integer</li>
 * <li>double &amp; Double</li>
 * <li>String</li>
 * <li>Java Bean with only properties of supported types</li>
 * <li>List of Java Beans</li>
 * </ul>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface TemplateModel extends Serializable {

    /**
     * Gets a proxy of the given part of the model as a bean of the given type.
     * Any changes made to the returned instance are reflected back into the
     * model.
     * <p>
     * You can use this for a type-safe way of updating a bean in the model. You
     * should not use this to update a database entity based on updated values
     * in the model.
     * <p>
     * The {@code modelPath} is a dot separated set of property names,
     * representing the location of the bean in the model. The root of the model
     * is "" and the path "person" represents what {@code getPerson()} would
     * return. The path "person.address" represents what
     * {@code getPerson().getAddress()} would return.
     *
     * <pre>
     * <code>
     * public class Address {
     *    private String street;
     *    public String getStreet(){
     *        return street;
     *    }
     *
     *    public void setStreet(String street){
     *        this.street = street;
     *    }
     * }
     *
     * public class Person {
     *    private String name;
     *    private Address address;
     *
     *    public String getName(){
     *        return name;
     *    }
     *
     *    public void setName(String name){
     *        this.name = name;
     *    }
     *
     *    public void setAddress(Address address){
     *       this.address = address;
     *    }
     *
     *    public Address getAddress(){
     *       return address;
     *    }
     * }
     * interface MyModel extends TemplateModel {
     *      Person getPerson();
     * }
     * </code>
     * </pre>
     *
     * @param <T>
     *            the proxy type
     * @param modelPath
     *            a dot separated path describing the location of the bean in
     *            the model
     * @param beanType
     *            requested bean type
     * @return a proxy instance of the bean found at the given {@code modelPath}
     */
    default <T> T getProxy(String modelPath, Class<T> beanType) {
        return TemplateModelUtil.resolveBeanAndRun(this, modelPath,
                (type, map) -> TemplateModelProxyHandler
                        .createModelProxy(map.getNode(), type.cast(beanType)));
    }

    /**
     * Gets a proxy of the given part of the model as a list of beans of the
     * given type. Any changes made to the returned instance are reflected back
     * into the model.
     * <p>
     * You can use this to update the collection or the contents of the
     * collection in the model.
     * <p>
     * The {@code modelPath} is a dot separated set of property names,
     * representing the location of the list in the model. The root of the model
     * is "" and the path "persons" represents what {@code List
     * <Person> getPersons()} would return.
     *
     * @param <T>
     *            the proxy type
     * @param modelPath
     *            a dot separated path describing the location of the list in
     *            the model
     * @param beanType
     *            requested bean type
     * @return a proxy instance of the list found at the given {@code modelPath}
     */
    default <T> List<T> getListProxy(String modelPath, Class<T> beanType) {
        return TemplateModelUtil.resolveListAndRun(this, modelPath,
                (type, list) -> new TemplateModelListProxy<>(list.getNode(),
                        type.getItemType().cast(beanType)));
    }

    /**
     * Import a bean properties passing the given filter to this template model.
     * <p>
     * The given filter should decide based on the bean property name whether
     * that property should be imported to the model. For nested bean
     * properties, the <em>dot annotation</em> is used.
     * <p>
     * For example, when the given <code>bean</code> is of type Person, and it
     * has a property <code>Address address</code>, the properties inside the
     * <code>Address</code> are passed to the given filter prefixed with
     * <code>address.</code>. e.g. <code>address.postCode</code>.
     *
     * @param modelPath
     *            a dot separated path describing the location of the bean in
     *            the model
     * @param bean
     *            the to import
     * @param propertyNameFilter
     *            the filter to apply to the bean's properties
     * @see TemplateModel supported property types
     */
    default void importBean(String modelPath, Object bean,
            Predicate<String> propertyNameFilter) {
        TemplateModelUtil.resolveBeanAndRun(this, modelPath, (type, map) -> {
            type.importProperties(map, bean,
                    new PropertyFilter(propertyNameFilter));

            return null;
        });
    }

    /**
     * Imports a list of beans to this template model.
     *
     * @param modelPath
     *            the path defining which part of the model to import into
     * @param beans
     *            the beans to import
     * @param propertyNameFilter
     *            a filter determining which bean properties to import
     *
     * @see #importBean(String, Object, Predicate)
     * @see TemplateModel supported property types
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    default void importBeans(String modelPath, List<?> beans,
            Predicate<String> propertyNameFilter) {
        TemplateModelUtil.resolveListAndRun(this, modelPath, (type, list) -> {
            type.importBeans(list, (List) beans,
                    new PropertyFilter(propertyNameFilter));

            return null;
        });
    }

}
