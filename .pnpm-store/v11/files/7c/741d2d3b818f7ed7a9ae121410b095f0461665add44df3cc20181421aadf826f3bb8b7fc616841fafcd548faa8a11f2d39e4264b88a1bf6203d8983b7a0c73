/*
  @license
	Rollup.js v4.63.3
	Mon, 14 Sep 2026 12:44:36 GMT - commit 250deca2945f8817350564f7d52bab0f79175d1b

	https://github.com/rollup/rollup

	Released under the MIT License.
*/
'use strict';

let fsEvents;
let fsEventsImportError;
async function loadFsEvents() {
    try {
        ({ default: fsEvents } = await import('fsevents'));
    }
    catch (error) {
        fsEventsImportError = error;
    }
}
// A call to this function will be injected into the chokidar code
function getFsEvents() {
    if (fsEventsImportError)
        throw fsEventsImportError;
    return fsEvents;
}

const fseventsImporter = /*#__PURE__*/Object.defineProperty({
    __proto__: null,
    getFsEvents,
    loadFsEvents
}, Symbol.toStringTag, { value: 'Module' });

exports.fseventsImporter = fseventsImporter;
exports.loadFsEvents = loadFsEvents;
//# sourceMappingURL=fsevents-importer.js.map
