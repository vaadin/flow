/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
const { describe, it } = intern.getPlugin('interface.bdd');
const { assert } = intern.getPlugin("chai");

import { DevTools } from "../../main/frontend/vaadin-dev-tools";

describe('vaadin-dev-tools', () => {

  it('should connect to port-hostname.gitpod.io with Spring Boot Devtools', () => {
    const devTools = document.createElement('vaadin-dev-tools');
    devTools.setAttribute('url', '');
    devTools.setAttribute('backend', 'SPRING_BOOT_DEVTOOLS');
    devTools.setAttribute('springBootLiveReloadPort', '35729');
    let location = {
      'protocol': 'https',
      'hostname': 'abc-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io'
    };
    assert.equal(devTools.getSpringBootWebSocketUrl(location),
      'ws://35729-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io');
  });

  it('should use base URI', () => {
    const devTools = document.createElement('vaadin-dev-tools');
    devTools.setAttribute('url', 'http://localhost:8080/context/vaadinServlet');
    devTools.setAttribute('backend', 'HOTSWAP_AGENT');

    assert.equal(devTools.getDedicatedWebSocketUrl(),
      'ws://localhost:8080/context/vaadinServlet?v-r=push&debug_window');
  });

  it('should have a new message on tray when error occurs', () => {
    const devTools = document.createElement('vaadin-dev-tools');
    devTools.setAttribute('url', '');
    devTools.setAttribute('backend', 'HOTSWAP_AGENT');
    devTools.openWebSocketConnection();
    devTools.javaConnection.handleError('TEST');
    assert.equal(devTools.javaStatus, 'error');
    assert.equal(devTools.messages.length, 1);
    assert.equal(devTools.messages[0].type, 'error');
    assert.equal(devTools.messages[0].message, 'TEST');

  });
});
