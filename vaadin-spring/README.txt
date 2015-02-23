Vaadin Spring
======================

Vaadin Spring is the official Spring integration for Vaadin Framework version 7.

Getting started
----
See the tutorial at https://vaadin.com/wiki/-/wiki/Main/Vaadin+Spring .

See also the companion add-on Vaadin Spring Boot.

Migrating from vaadin4spring
----
Vaadin Spring contains a subset of the functionality of the add-on vaadin4spring.
The community add-on vaadin4spring can still be used for the functionality not present in Vaadin Spring.

As the naming of packages and some annotations have changed, the following renames and import updates are typically required for migration:
* org.vaadin.spring -> com.vaadin.spring (for the annotations and classes included in Vaadin Spring)
* @VaadinUI -> @SpringUI("")
  * Note that an empty string or "/" should now be used as the parameter for the root UI!
* @VaadinView -> @SpringView

Note also that the ui parameter of @VaadinView now also covers the subclasses of the listed UI classes.

Issue tracking
----
Issues for the add-on are tracked in the Vaadin Trac at http://dev.vaadin.com

Building Vaadin Spring
----
See the parent project page at https://github.com/vaadin/spring .

Contributions
----
Contributions to the project can be done using the Vaadin Gerrit review system at http://dev.vaadin.com/review. Pull requests cannot be accepted due to Gerrit being used for code review.


Copyright 2015 Vaadin Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
