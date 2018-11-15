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
package com.vaadin.flow.router;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * XXX: Alternative name : ScopedRouteRegistry
 *
 * @author Vaadin Ltd
 *
 */
public class RouteRegistry {

    private static final RouteRegistry APPLICATION_REGISTRY = new RouteRegistry();

    private final VaadinSession session;

    /**
     * Application scoped route registry should be initialized when it asked for
     * the info the very first time (remember about OSGi: don't initialize
     * anything at bootstrap phase because OSGi simply just don't have this
     * phase at all).
     *
     * {@link com.vaadin.flow.server.startup.RouteRegistry} should be used as a
     * low level source of info for the application scoped route registry: all
     * info available statically via annotations should be translated to the
     * data which this route registry may accept.
     *
     * As a result there should not be public code which reads route info from
     * annotations (and used inside Router implementation). Every time when
     * router info is needed only scoped route registry should be used to ask
     * for it.
     *
     * {@link com.vaadin.flow.server.startup.RouteRegistry} will be used as a
     * route data collector only. It should pass the data to application scoped
     * registry via its API methods and never participates in route data
     * retrieval after that.
     */
    private RouteRegistry() {
        session = null;
    }

    private RouteRegistry(VaadinSession session) {
        this.session = session;
    }

    public static RouteRegistry getApplicationRegistry() {
        // Alternative: return a subclass instead of RouteRegistry
        return APPLICATION_REGISTRY;
    }

    public static RouteRegistry getSessionRegistry(VaadinSession session) {
        RouteRegistry registry = session.getAttribute(RouteRegistry.class);
        if (registry == null) {
            // Alternative: use a subclass specific for session scope instead of
            // RouteRegistry
            registry = new RouteRegistry(session);
            session.setAttribute(RouteRegistry.class, registry);
        }
        return registry;
    }

    // Methods that allows to add new route hierarchy/change existing
    // hierarchy

    /**
     * the implementation of this method should calculate also themes for the
     * route target based on chain info (this is the only info available here
     * since routes related annotations are just not available).
     */
    public void addRoute(String path, Class<? extends Component> routeTarget,
            Class<? extends RouterLayout>... ascendants) {
        // this is equivalent of
        // @formatter:off
        /*
         *
         * @Route(path, layout = ascendants[0].class)
         * class routerTarget extends Component {
         * }
         *
         * @ParentLayout(ascenands[1].class)
         * class ascendants[0] implements RouterLayout {
         * }
         *
         * and so on
         */
        // @formatter:on

        // if the routeTarget already has a path this should remove the existing
        // path and replace it with the new one
    }

    /**
     *
     * Route alias behaves exactly the same as route from the point of view of
     * navigation to the component.
     *
     * The only difference between route and route alias is
     * {@link Router#getUrl(Class)} method which returns the URL considering
     * only route (ignoring route aliases).
     */
    public void addRouteAlias(String path,
            Class<? extends Component> routeTarget,
            // The problem with this vararg parameter: arrays (which is the
            // resulting type of the parameter) is not aware of it item types.
            // As a result you may use any Class<?> you want here.
            Class<? extends RouterLayout>... ascendants) {
        // same as above but just for aliases

        // if we want to have this method with varargs parameter then we should
        // do runtime check of the array items
    }

    /**
     * The type safe method version.
     */
    public void addRouteAlias(String path,
            Class<? extends Component> routeTarget,
            List<Class<? extends RouterLayout>> ascendants) {
        // same as above but just for aliases
    }

    public void setRoute(String path, Class<? extends Component> routeTarget,
            boolean reuseHierarchy) {
        // same as above except:
        //
        // if reuseHierarchy is {@code true}
        //
        // then in case there was already a
        // chain of ascendants made by the method above
        // (or via number of setParentLayout method) then this hierarchy will be
        // kept. So effectively only {@code path} is updated
        //
        // if reuseHierarchy is {@code true}
        //
        // then the routeTarget is detached from the previous hierarchy (which
        // doesn't mean that a chain of descendants should be removed)
    }

    /*
     * It looks like there is no sense to have methods which allows to set route
     * prefix for parent layout ( see RoutePrefix ). You always may just use a
     * full {@code path} (which already contains all prefixes for the parent
     * layouts).
     *
     * Otherwise API looks very complicated with rare usecases.
     */

    /**
     * I'm not sure whether this method is needed at all.
     *
     * The problem with it: it modifies a chain of parents. But there is a
     * question: which chain it modifies ?
     *
     * If I have one route A with parent is B whose parent is C. (A-B-C)
     *
     * And another route Z with parent Y whose parent is B. (Z-Y-B)
     *
     * What happens if I call this method as {@link #setParentLayout(B.class,
     * W.class)}? It should modify both chains but this may be confusing.
     *
     * So instead of allowing to modifying a chain somewhere in middle we should
     * allows to modify all the chain starting from the route. (So the chain is
     * identified by the route target).
     *
     * XXX: to remove
     */
    public void setParentLayout(
            Class<? extends Component> routeHierarchyComponent,
            Class<? extends RouterLayout> parent) {
        // this is equivalent of
        // @formatter:off
        /*
         * @Route(some_already_defined_path, layout = parent.class)
         * class routeHierarchyComponent extends Component {
         * }
         * if the routeHierarchyComponent is the route target
         * (the leaf in the route hierarchy)
         *
         * or
         *
         * @ParentLayout(parent.class)
         * class routeHierarchyComponent implements RouterLayout {
         * }
         * if the routeHierarchyComponent is interim router layout
         * (somewhere inside a route hierarrchy, not leaf)
         */
        // @formatter:on
    }

    // Removes the existing routes

    /**
     * It's not enough to remove the route from the store of the registry
     * itself. The route may be in a wider scope (e.g. global registry). The
     * wider scope should not be modified from the narrowed scope. Instead the
     * current registry should save the info that this route is removed and
     * don't return it via getters even if the wider scope has this route.
     *
     * But if the wider scope has a route which has not been removed explicitly
     * from the narrowed scope then getter should return it.
     */
    public void removeRoute(String path) {
        removeRoute(path, false);
    }

    /**
     * If we decide do not have methods which modifies only a part of chain then
     * this method should be removed. Only {@link RouteRegistry#removeRoute}
     * should stay and it always should remove the route along with its chain
     * (there should not stay any chain whose head is not a target route).
     *
     * XXX: to remove
     */
    public void removeRoute(String path, boolean removeAscendantsChain) {
        // removes {@code path} from the routes and detaches the corresponding
        // route target component
        //
        // if {@code removeAscendantsChain} is {@code true} then all ascendants
        // chain is removed
        //
        // if {@code removeAscendantsChain} is {@code true} then ascendants
        // chain is not modified (so it can be reused later on)
    }

    public void removeRoute(Class<? extends Component> clazz) {
        // removes the route associated with the clazz route target
        // should remove also route aliases
    }

    /**
     * This method should be removed if we decide do not have
     * {@link #setParentLayout(Class, Class)}
     *
     * XXX: to remove
     */
    public void unsetParentLayout(
            Class<? extends Component> routeHierarchyComponent) {
        // see {@code setParentLayout}. Breaks the ascendants chain (removes the
        // parent for the routeHierarchyComponent).
    }

    // Getters

    /**
     * Every getter should first use the router registry instance on which it's
     * called and then calls the same method on the nearest router registry
     * scope: e.g. if a {@code getXXX} method is called on the session scoped
     * router registry then it should check whether there is a value in this
     * registry and then ask applicaton scoped registry
     */

    /**
     * If we decide do not have {@link #setParentLayout(Class, Class)} method
     * then we should always return a chain for the route target. XXX : to
     * remove
     */
    public Optional<Class<? extends RouterLayout>> getParentLayout(
            Class<? extends Component> clazz) {
        // returns the parent layout for the component clazz which is either a
        // router target or a parent layout itself, see {@code setParentLayout}
        return null;
    }

    public List<Class<? extends RouterLayout>> getParentLayoutChain(
            Class<? extends Component> clazz) {
        // returns ascendants chain starting from the {@code clazz}.
        // clazz may be either a router target or router layout
        return null;
    }

    /**
     * see {@link RouteRegistry#getNavigationTarget(String)}
     */
    public Optional<Class<? extends Component>> getNavigationTarget(
            String path) {
        return null;
    }

    /**
     * see {@link RouteRegistry#getRegisteredRoutes()}
     *
     * It might be that RouteData class should be changed/improved. Currently
     * this class contains only info about the target itself and its parent. All
     * other information can be caclulated on the fly based on annotations.
     *
     * With dynamic routes there is no any information available via annotations
     * which means that RouteData class here should contain all this info right
     * away.
     */
    public List<RouteData> getRegisteredRoutes() {
        return null;
    }

    /**
     * see {@link RouteRegistry#hasRouteTo(String)}
     */
    public boolean hasRouteTo(String pathString) {
        return false;
    }

    /**
     * see {@link RouteRegistry#getTargetUrl(Class)}
     */
    public Optional<String> getTargetUrl(
            Class<? extends Component> navigationTarget) {
        return null;
    }

    /**
     *
     * see {@link RouteRegistry#getThemeFor(Class, String)}
     */
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {
        return null;
    }

    /*
     * Other methods from RouteRegistry should be revised.
     *
     * It might be they are not needed to be here at all or they should be
     * included as well.
     */
}
