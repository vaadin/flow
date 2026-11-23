import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';

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
