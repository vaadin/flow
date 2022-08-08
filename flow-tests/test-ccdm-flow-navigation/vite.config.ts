import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  server: {
    hmr: {
        timeout: 5000
    }
  },
  plugins: [
      {
          name: "vaadin:test-offline-mode-workarounds",
          enforce: 'post',
          transform: (code: string, id: string) => {
            const [bareId, query] = id.split('?');
            if (bareId.endsWith("/@vite-plugin-checker-runtime")) {
                // Disable vite-plugin-checker websocket connection
                // to avoid browser reloads
                return "export function inject() { }";
            } else if (bareId.includes("/vite/dist/client/client")) {
                // Dirty workaround to prevent infinite reload
                // Can be removed when switching to Vite 3
                // https://github.com/vitejs/vite/pull/6819/files
                var out = code.replace(
                    "const pingResponse = await fetch(`${base}__vite_ping`);",
                    "const pingResponse = await fetch(`${location.protocol}//${socketHost}`)"
                );
                return out;
            }
          }
      }
  ]
});

export default overrideVaadinConfig(customConfig);
