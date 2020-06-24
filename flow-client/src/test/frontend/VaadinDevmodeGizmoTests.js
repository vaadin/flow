const { describe, it } = intern.getPlugin('interface.bdd');
const { assert } = intern.getPlugin("chai");

import { init } from "../../main/resources/META-INF/resources/frontend/VaadinDevmodeGizmo";

describe('VaadinDevmodeGizmo', () => {

  it('should connect to port-hostname.gitpod.io with Spring Boot Devtools', () => {
    let gizmo = init( 'http://localhost:8080', 'SPRING_BOOT_DEVTOOLS', 35729);
    let location = {
      'protocol': 'https',
      'hostname': 'abc-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io'
    };
    assert.equal(gizmo.getSpringBootWebSocketUrl(location),
      'ws://35729-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io');
  });

  it('should use base URI', () => {
    let gizmo = init('http://localhost:8080/context/vaadinServlet','HOTSWAP_AGENT', 35729);
    assert.equal(gizmo.getDedicatedWebSocketUrl(),
      'ws://localhost:8080/context/vaadinServlet?v-r=push&refresh_connection');
  });

  it('should have a new message on tray when error occurs', () => {
    let gizmo = init('http://localhost:8080','SPRING_BOOT_DEVTOOLS', 35729);
    gizmo.connection.onerror('TEST');
    let message = {
      'id': 1,
      'type': 'error',
      'message': 'TEST',
      'details': null,
      'link': null
    };
    assert.equal(gizmo.status, 'error');
    assert.equal(gizmo.messages.length, 1);
    assert.deepEqual(gizmo.messages[0], message);
  });
});
