import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

import { createFilter } from "rollup-pluginutils";
import path from 'path';
import litcss from 'rollup-plugin-lit-css';
import postcssLit from 'rollup-plugin-postcss-lit';
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
              console.log("==================== rollup-plugin-lit-css (FAKE) " + id + " --> accepted? " + myfilter(id));
            const [bareId, query] = id.split('?');
            //if (!bareId.endsWith(".css")) {
            if (!id.endsWith(".css")) {
                return;
            }
                
                  console.log("===================== myplugin : " + id + " --> " + options + " --> " + raw);
                  return;
          }
      },
      {
        ...litcss({exclude: ["/**/index.html*"] })
        //...litcss()
        , enforce: 'post'
      },
      /*
      */
      {
          name: "marco:testme2",
          enforce: 'post',
          transform(raw, id, options) {
              console.log("==================== rollup-plugin-lit-css (FAKE2) " + id + " --> accepted? " + myfilter(id));
            const [bareId, query] = id.split('?');
            //if (!bareId.endsWith(".css")) {
            if (!id.endsWith(".css")) {
                return;
            }
                
                  console.log("===================== myplugin2 : " + id + " --> " + options + " --> " + raw);
                  return;
          }
      },
      /*
      */
  ]
});

export default overrideVaadinConfig(customConfig);
