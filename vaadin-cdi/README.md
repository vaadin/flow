# vaadin-cdi

Vaadin-CDI is the official CDI integration for Vaadin Framework.

## Version 10.0

Rebuilt for Vaadin 10 based on the previous addon versions.

### Startup

If you do not customize Vaadin Servlet in your web.xml, 
a CDI enabled Vaadin servlet is deployed automatically. 

Otherwise you can customize 
[CdiVaadinServlet](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/CdiVaadinServlet.java) 
just like VaadinServlet.

### Component instantiation and CDI

Vaadin triggered instantiation happens in a 
[CDI aware Vaadin Instantiator](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/CdiInstantiator.java) 
implementation. 
These components are created by the instantiator:

- `@Route`, RouteLayout, HasErrorParameter components
- components injected by `@Id` into polymer templates

By default instantiator looks up the CDI bean by type ( component class ), 
and gets a contextual reference from BeanManager. 
All the CDI features are usable like observer, interceptor, decorator.

When type is not found as a CDI bean 
( for example ambiguous, or does not have a no-arg public constructor ), 
instantiation falls back to the default Vaadin behavior. 
After the instantiation, dependency injection is performed. 
Injects work, but other CDI features are not, because instantiated component is not a contextual instance. 

### Vaadin Contexts

#### VaadinServiceScoped

[@VaadinServiceScoped](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/annotation/VaadinServiceScoped.java) 
is a normal ( proxied ) scope. Its purpose to define a scope for the beans used by VaadinService. Like an Instantiator, or an I18NProvider.   

#### VaadinSessionScoped

[@VaadinSessionScoped](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/annotation/VaadinSessionScoped.java) 
is a normal ( proxied ) scope. Every VaadinSession have a separate Context. 

#### UIScoped, NormalUIScoped

Every UI has a separate Context. 
Practically it means there is just one instance per UI for the scoped class.

For components, use [@UIScoped](../../master/vaadin-cdi/src/main/java/com/vaadin/annotation/cdi/UIScoped.java). 
It is a pseudo scope, so gives a direct reference. 
Vaadin component tree does not work properly with CDI client proxies.

For other beans you can use 
[@NormalUIScoped](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/annotation/NormalUIScoped.java). 
Given it is normal scoped, have some benefit. 
For example can handle cyclic dependency.

#### RouteScoped, NormalRouteScoped 

[@RouteScoped](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/annotation/RouteScoped.java) context lifecycle on its own is same as UI context's. 
Together with the concept of [@RouteScopeOwner](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/annotation/RouteScopeOwner.java) it can be used to bind beans to router components (target/layout/exceptionhandler).
Until owner remains in the route, all beans owned by it remain in the scope.
 
Same as before, for vaadin components use `@RouteScoped`, it is a pseudo scope.
For other beans you can use `@NormalRouteScoped`, it is a normal scope.
 
### Services

Some Vaadin service interfaces can be implemented as a CDI bean.

- I18NProvider
- Instantiator
- SystemMessagesProvider
- ErrorHandler

Beans have to be qualifed by 
[@VaadinServiceEnabled](../../master/vaadin-cdi/src/main/java/com/vaadin/cdi/annotation/VaadinServiceEnabled.java) 
to be picked up automatically.

### Vaadin Events

The following events are fired as a CDI event:

- ServiceInitEvent
- PollEvent
- BeforeEnterEvent
- BeforeLeaveEvent
- AfterNavigationEvent
- UIInitEvent
- SessionInitEvent
- SessionDestroyEvent
- ServiceDestroyEvent

You just need a CDI observer to handle them.

### Known issues and limitations

#### Custom UI

As of V10 no custom UI subclass is needed for the application.
You can define one by the corresponding servlet parameter, 
but it is instantiated by the framework as a POJO.

You should not need a custom UI subclass. Though dependency injection can be achieved, just in case.
Use CDI BeanManager in `UI.init`. Through Deltaspike's `BeanProvider.injectFields(this)` for example.

#### ServiceDestroyEvent

During application shutdown it is implementation specific, 
whether it works with CDI or not. 
But according to servlet specs, 
a servlet destroy ( it means a service destroy too ) can happen in 
other circumstances too.

#### Push with CDI

Vaadin contexts are usable inside `UI.access` with any push transport.

But an incoming websocket message does not count as a request in CDI. 
Need a http request to have request, session, and conversation context. 

So you should use WEBSOCKET_XHR (it is the default), or LONG_POLLING 
transport, otherwise you lost these standard contexts. 

In background threads these contexts are not active regardless of push.

#### Router and CDI

Vaadin scans router classes (targets, layouts) without any clue about CDI beans. 
Using producers, or excluding the bean class from types with `@Typed` causes issues with these kind of beans.

#### Instantiator and CDI Qualifiers

As you can see at component instantiation, beans looked up by bean type. 
The API can not provide qualifiers, so lookup is done with `@Any`.

---

Copyright 2012-2018 Vaadin Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
