import File = require('vinyl');
import { AsyncTransformStream } from './streams';
/**
 * Wraps `addCustomElementsEs5Adapter()` in a `stream.Transform`.
 */
export declare class CustomElementsEs5AdapterInjector extends AsyncTransformStream<File, File> {
    constructor();
    protected _transformIter(files: AsyncIterable<File>): AsyncIterable<File>;
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
export declare function addCustomElementsEs5Adapter(html: string): string;
