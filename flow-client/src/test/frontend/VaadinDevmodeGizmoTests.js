const { describe, it } = intern.getPlugin('interface.bdd');
const { assert } = intern.getPlugin("chai");

import { VaadinDevmodeGizmo } from "../../main/frontend/VaadinDevmodeGizmo";

describe('VaadinDevmodeGizmo', () => {

  it('should connect to port-hostname.gitpod.io with Spring Boot Devtools', () => {
    const gizmo = document.createElement('vaadin-devmode-gizmo');
    gizmo.setAttribute('url', '');
    gizmo.setAttribute('backend', 'SPRING_BOOT_DEVTOOLS');
    gizmo.setAttribute('springBootLiveReloadPort', '35729');
    let location = {
      'protocol': 'https',
      'hostname': 'abc-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io'
    };
    assert.equal(gizmo.getSpringBootWebSocketUrl(location),
      'ws://35729-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io');
  });

  it('should use base URI', () => {
    const gizmo = document.createElement('vaadin-devmode-gizmo');
    gizmo.setAttribute('url', 'http://localhost:8080/context/vaadinServlet');
    gizmo.setAttribute('backend', 'HOTSWAP_AGENT');

    assert.equal(gizmo.getDedicatedWebSocketUrl(),
      'ws://localhost:8080/context/vaadinServlet?v-r=push&refresh_connection');
  });

  it('should have a new message on tray when error occurs', () => {
    const gizmo = document.createElement('vaadin-devmode-gizmo');
    gizmo.setAttribute('url', '');
    gizmo.setAttribute('backend', 'HOTSWAP_AGENT');
    gizmo.openWebSocketConnection();
    gizmo.javaConnection.handleError('TEST');
    assert.equal(gizmo.javaStatus, 'error');
    assert.equal(gizmo.messages.length, 1);
    assert.equal(gizmo.messages[0].type, 'error');
    assert.equal(gizmo.messages[0].message, 'TEST');

  });
});
