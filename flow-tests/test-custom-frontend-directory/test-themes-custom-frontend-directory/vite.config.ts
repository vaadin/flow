import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

import { createFilter } from "rollup-pluginutils";
import path from 'path';
import litcss from 'rollup-plugin-lit-css';
import settings from './target/vaadin-dev-server-settings.json';
const frontendFolder = path.resolve(__dirname, settings.frontendFolder);
const frontendGeneratedFolder = path.resolve(frontendFolder, settings.generatedFolder);
const xxx = path.relative(__dirname, frontendGeneratedFolder);
const myfilter = createFilter(/\.css$/i, [xxx + "/**/*.css"]);

console.log("=SDFSDFSDFSDFSDFDSFSD ========================= " + xxx);

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  plugins: [
      {
          name: "marco:testme",
          enforce: 'post',
          transform(raw, id, options) {
            const [bareId, query] = id.split('?');
            if (!bareId.endsWith(".css")) {
                return;
            }
              console.log("==================== rollup-plugin-lit-css (FAKE) " + id + " --> accepted? " + myfilter(id));
                
                  //console.log("===================== myplugin : " + id + " --> " + options + " --> " + raw);
                  return;
          }
      },
      {
        ...litcss({exclude: [xxx + "/**/*.css"] })
        , enforce: 'post'
      },
      /*
      */
  ]
});

export default overrideVaadinConfig(customConfig);
