import * as dom5 from 'dom5/lib/index-next';
import {predicates as p} from 'dom5/lib/index-next';
import * as parse5 from 'parse5';
import * as url from 'url';

import File = require('vinyl');
import {AsyncTransformStream, getFileContents} from './streams';

const attrValueMatches = (attrName: string, regex: RegExp) => {
  return (node: parse5.ASTNode) => {
    const attrValue = dom5.getAttribute(node, attrName);
    return attrValue != null && regex.test(attrValue);
  };
};

const webcomponentsLoaderRegex = /\bwebcomponents\-(loader|lite|bundle)\.js\b/;
const webcomponentsLoaderMatcher = p.AND(
    p.hasTagName('script'), attrValueMatches('src', webcomponentsLoaderRegex));

/**
 * Wraps `addCustomElementsEs5Adapter()` in a `stream.Transform`.
 */
export class CustomElementsEs5AdapterInjector extends
    AsyncTransformStream<File, File> {
  constructor() {
    super({objectMode: true});
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      if (file.contents === null || file.extname !== '.html') {
        yield file;
        continue;
      }
      const contents = await getFileContents(file);
      const updatedContents = addCustomElementsEs5Adapter(contents);
      if (contents === updatedContents) {
        yield file;
      } else {
        const updatedFile = file.clone();
        updatedFile.contents = Buffer.from(updatedContents, 'utf-8');
        yield updatedFile;
      }
    }
  }
}

/**
 * Please avoid using this function because the API is likely to change. Prefer
 * the interface provided by `PolymerProject.addCustomElementsEs5Adapter`.
 *
 * When compiling ES6 classes down to ES5 we need to include a special shim so
 * that compiled custom elements will still work on browsers that support native
 * custom elements.
 *
 * TODO(fks) 03-28-2017: Add tests.
 */
export function addCustomElementsEs5Adapter(html: string): string {
  // Only modify this file if we find a web components polyfill. This is a
  // heuristic to identify the entry point HTML file. Ultimately we should
  // explicitly transform only the entry point by having the project config.
  if (!webcomponentsLoaderRegex.test(html)) {
    return html;
  }
  const parsed = parse5.parse(html, {locationInfo: true});
  const script = dom5.query(parsed, webcomponentsLoaderMatcher);
  if (!script) {
    return html;
  }

  // Collect important dom references & create fragments for injection.
  const loaderScriptUrl = dom5.getAttribute(script, 'src')!;
  const adapterScriptUrl =
      url.resolve(loaderScriptUrl, 'custom-elements-es5-adapter.js');
  const es5AdapterFragment = parse5.parseFragment(`
    <script>if (!window.customElements) { document.write('<!--'); }</script>
    <script type="text/javascript" src="${adapterScriptUrl}"></script>
    <!--! do not remove -->
`);

  dom5.insertBefore(script.parentNode!, script, es5AdapterFragment);
  return parse5.serialize(parsed);
}
