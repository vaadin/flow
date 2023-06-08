import {fileURLToPath} from 'url';
import {hmrPlugin, presets} from '@open-wc/dev-server-hmr';
import {esbuildPlugin} from '@web/dev-server-esbuild';
import cors from '@koa/cors';
import { fromRollup } from '@web/dev-server-rollup';
import rollupCommonjs from '@rollup/plugin-commonjs';

const commonjs = fromRollup(rollupCommonjs);

export default {
  plugins: [
    // Transpile TS to JS, using existing TS config
    esbuildPlugin({
      ts: true,
      tsconfig: fileURLToPath(new URL('./tsconfig.json', import.meta.url)),
    }),
    // Enable hot module reload for dev tools components
    hmrPlugin({
      include: ['frontend/**/*'],
      presets: [presets.lit]
    }),

    commonjs({
      include: [
        // the commonjs plugin is slow, list the required packages explicitly:
        '**/node_modules/accessibility-checker/**',
      ],
    }),
  ],
  middleware: [
    // Configure CORS so that local dev tools can be loaded from a Flow
    // application, the app needs to run on port 8080
    cors({
      origin: 'http://localhost:8080'
    })
  ]
};
