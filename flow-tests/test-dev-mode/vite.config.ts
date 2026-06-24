/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => ({
  plugins: [
    {
      name: 'communication-test',
      configureServer(server) {
        server.ws.on('test-event', (data) => {
          server.ws.send('test-event-response', data);
        });
      }
    }
  ]
});

export default overrideVaadinConfig(customConfig);
