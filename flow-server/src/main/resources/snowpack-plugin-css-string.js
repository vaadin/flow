// This is a generated file. It will be overwritten by Flow.

// This is the recommended way to get CSS string exported from the module:
// https://github.com/pikapkg/snowpack/issues/580#issuecomment-683323847
const { promises: fs } = require("fs");

module.exports = function plugin(snowpackConfig, options = {}) {
  return {
    name: 'snowpack-plugin-css-string',
    resolve: {
      input: options.input || ['.css'],
      output: ['.js'],
    },
    async load({filePath}) {
      const contents = await fs.readFile(filePath, "utf-8");
      return `
        import { css } from 'lit-element';
        export default css\`${ contents.replace(/(`|\\|\${)/g, '\\$1') }\`;
      `;
    },
  };
};