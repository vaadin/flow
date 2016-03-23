/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server;

import java.io.Serializable;
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< HEAD
=======
>>>>>>> f4adc2d Corrections.
import java.net.URI;

/**
 * Stream resource registration result.
 * <p>
 * Use {@link #getResourceUri()} to get URI after {@link StreamResource} is
 * registered. It also allows to unregister the resource.
 * 
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
=======

/**
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
>>>>>>> ae80070 Some javadocs.
 * @author Vaadin Ltd
 *
 */
public interface StreamResourceRegistration extends Serializable {

<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< HEAD
    /**
     * Get resource URI for registered {@link StreamResource} instance.
     * <p>
     * The URI is relative to the application base URI.
     * 
     * @return resource URI
     */
    URI getResourceUri();

    /**
     * Unregister {@link StreamResource}.
     * <p>
     * The resource will be removed from the session and its URI won't be served
     * by the application anymore so that the resource becomes available for GC.
     * <p>
     * It's the developer's responsibility to call this method at the
     * appropriate time. Otherwise the resource instance will stay in memory
     * until the session expires.
     */
=======
=======
    /**
     * Get resource URI for registered {@link StreamResource} instance.
     * <p>
     * The URI is relative to the application base URI.
     * 
     * @return resource URI
     */
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
>>>>>>> ae80070 Some javadocs.
    String getResourceUri();
=======
    String getResourceUrl();
>>>>>>> 542ad4a Review based fixes.
=======
    URI getResourceUri();
>>>>>>> f4adc2d Corrections.

<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
    /**
     * Unregister {@link StreamResource}.
     * <p>
     * The resource will be removed from the session and its URI won't be served
     * by the application anymore so that the resource becomes available for GC.
     * <p>
     * It's the developer's responsibility to call this method at the
     * appropriate time. Otherwise the resource instance will stay in memory
     * until the session expires.
     */
>>>>>>> ae80070 Some javadocs.
    void unregister();
}
