const { describe, it } = intern.getPlugin('interface.bdd');
const { assert } = intern.getPlugin("chai");

import { init } from "../../main/resources/META-INF/resources/frontend/VaadinDevmodeGizmo";

describe('VaadinDevmodeGizmo', () => {

  it('should connect to port-hostname.gitpod.io with Spring Boot Devtools', () => {
    let gizmo = init(undefined, '','SPRING_BOOT_DEVTOOLS', 35729);
    let location = {
      'protocol': 'https',
      'hostname': 'abc-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io'
    };
    assert.equal(gizmo.getSpringBootWebSocketUrl(location),
      'ws://35729-12345678-1234-1234-1234-1234567890ab.ws-eu01.gitpod.io');
  });

  it('should append push target if given', () => {
    let gizmo = init(undefined, 'context/vaadinServlet','HOTSWAP_AGENT', 35729);
    let location = {
      'href': 'http://localhost:8080/context/myroute',
      'protocol': 'http:',
      'host': 'localhost:8080',
    };
    assert.equal(gizmo.getDedicatedWebSocketUrl(location),
      'ws://localhost:8080/context/vaadinServlet?v-r=push&refresh_connection');
  });
});
