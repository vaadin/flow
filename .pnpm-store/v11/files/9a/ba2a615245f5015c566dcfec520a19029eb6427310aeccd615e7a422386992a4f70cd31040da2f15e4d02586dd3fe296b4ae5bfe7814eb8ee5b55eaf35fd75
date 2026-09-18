/*istanbul ignore start*/'use strict';

exports.__esModule = true;
exports.canonicalize = exports.convertChangesToXML = exports.convertChangesToDMP = exports.merge = exports.parsePatch = exports.applyPatches = exports.applyPatch = exports.createPatch = exports.createTwoFilesPatch = exports.structuredPatch = exports.diffArrays = exports.diffJson = exports.diffCss = exports.diffSentences = exports.diffTrimmedLines = exports.diffLines = exports.diffWordsWithSpace = exports.diffWords = exports.diffChars = exports.Diff = undefined;

/*istanbul ignore end*/var /*istanbul ignore start*/_base = require('./diff/base') /*istanbul ignore end*/;

/*istanbul ignore start*/var _base2 = _interopRequireDefault(_base);

/*istanbul ignore end*/var /*istanbul ignore start*/_character = require('./diff/character') /*istanbul ignore end*/;

var /*istanbul ignore start*/_word = require('./diff/word') /*istanbul ignore end*/;

var /*istanbul ignore start*/_line = require('./diff/line') /*istanbul ignore end*/;

var /*istanbul ignore start*/_sentence = require('./diff/sentence') /*istanbul ignore end*/;

var /*istanbul ignore start*/_css = require('./diff/css') /*istanbul ignore end*/;

var /*istanbul ignore start*/_json = require('./diff/json') /*istanbul ignore end*/;

var /*istanbul ignore start*/_array = require('./diff/array') /*istanbul ignore end*/;

var /*istanbul ignore start*/_apply = require('./patch/apply') /*istanbul ignore end*/;

var /*istanbul ignore start*/_parse = require('./patch/parse') /*istanbul ignore end*/;

var /*istanbul ignore start*/_merge = require('./patch/merge') /*istanbul ignore end*/;

var /*istanbul ignore start*/_create = require('./patch/create') /*istanbul ignore end*/;

var /*istanbul ignore start*/_dmp = require('./convert/dmp') /*istanbul ignore end*/;

var /*istanbul ignore start*/_xml = require('./convert/xml') /*istanbul ignore end*/;

/*istanbul ignore start*/function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

/* See LICENSE file for terms of use */

/*
 * Text diff implementation.
 *
 * This library supports the following APIS:
 * JsDiff.diffChars: Character by character diff
 * JsDiff.diffWords: Word (as defined by \b regex) diff which ignores whitespace
 * JsDiff.diffLines: Line based diff
 *
 * JsDiff.diffCss: Diff targeted at CSS content
 *
 * These methods are based on the implementation proposed in
 * "An O(ND) Difference Algorithm and its Variations" (Myers, 1986).
 * http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.4.6927
 */
exports. /*istanbul ignore end*/Diff = /*istanbul ignore start*/_base2 /*istanbul ignore end*/[/*istanbul ignore start*/'default' /*istanbul ignore end*/];
/*istanbul ignore start*/exports. /*istanbul ignore end*/diffChars = /*istanbul ignore start*/_character /*istanbul ignore end*/. /*istanbul ignore start*/diffChars;
exports. /*istanbul ignore end*/diffWords = /*istanbul ignore start*/_word /*istanbul ignore end*/. /*istanbul ignore start*/diffWords;
exports. /*istanbul ignore end*/diffWordsWithSpace = /*istanbul ignore start*/_word /*istanbul ignore end*/. /*istanbul ignore start*/diffWordsWithSpace;
exports. /*istanbul ignore end*/diffLines = /*istanbul ignore start*/_line /*istanbul ignore end*/. /*istanbul ignore start*/diffLines;
exports. /*istanbul ignore end*/diffTrimmedLines = /*istanbul ignore start*/_line /*istanbul ignore end*/. /*istanbul ignore start*/diffTrimmedLines;
exports. /*istanbul ignore end*/diffSentences = /*istanbul ignore start*/_sentence /*istanbul ignore end*/. /*istanbul ignore start*/diffSentences;
exports. /*istanbul ignore end*/diffCss = /*istanbul ignore start*/_css /*istanbul ignore end*/. /*istanbul ignore start*/diffCss;
exports. /*istanbul ignore end*/diffJson = /*istanbul ignore start*/_json /*istanbul ignore end*/. /*istanbul ignore start*/diffJson;
exports. /*istanbul ignore end*/diffArrays = /*istanbul ignore start*/_array /*istanbul ignore end*/. /*istanbul ignore start*/diffArrays;
exports. /*istanbul ignore end*/structuredPatch = /*istanbul ignore start*/_create /*istanbul ignore end*/. /*istanbul ignore start*/structuredPatch;
exports. /*istanbul ignore end*/createTwoFilesPatch = /*istanbul ignore start*/_create /*istanbul ignore end*/. /*istanbul ignore start*/createTwoFilesPatch;
exports. /*istanbul ignore end*/createPatch = /*istanbul ignore start*/_create /*istanbul ignore end*/. /*istanbul ignore start*/createPatch;
exports. /*istanbul ignore end*/applyPatch = /*istanbul ignore start*/_apply /*istanbul ignore end*/. /*istanbul ignore start*/applyPatch;
exports. /*istanbul ignore end*/applyPatches = /*istanbul ignore start*/_apply /*istanbul ignore end*/. /*istanbul ignore start*/applyPatches;
exports. /*istanbul ignore end*/parsePatch = /*istanbul ignore start*/_parse /*istanbul ignore end*/. /*istanbul ignore start*/parsePatch;
exports. /*istanbul ignore end*/merge = /*istanbul ignore start*/_merge /*istanbul ignore end*/. /*istanbul ignore start*/merge;
exports. /*istanbul ignore end*/convertChangesToDMP = /*istanbul ignore start*/_dmp /*istanbul ignore end*/. /*istanbul ignore start*/convertChangesToDMP;
exports. /*istanbul ignore end*/convertChangesToXML = /*istanbul ignore start*/_xml /*istanbul ignore end*/. /*istanbul ignore start*/convertChangesToXML;
exports. /*istanbul ignore end*/canonicalize = /*istanbul ignore start*/_json /*istanbul ignore end*/. /*istanbul ignore start*/canonicalize;
/*istanbul ignore end*/
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uL3NyYy9pbmRleC5qcyJdLCJuYW1lcyI6WyJEaWZmIiwiZGlmZkNoYXJzIiwiZGlmZldvcmRzIiwiZGlmZldvcmRzV2l0aFNwYWNlIiwiZGlmZkxpbmVzIiwiZGlmZlRyaW1tZWRMaW5lcyIsImRpZmZTZW50ZW5jZXMiLCJkaWZmQ3NzIiwiZGlmZkpzb24iLCJkaWZmQXJyYXlzIiwic3RydWN0dXJlZFBhdGNoIiwiY3JlYXRlVHdvRmlsZXNQYXRjaCIsImNyZWF0ZVBhdGNoIiwiYXBwbHlQYXRjaCIsImFwcGx5UGF0Y2hlcyIsInBhcnNlUGF0Y2giLCJtZXJnZSIsImNvbnZlcnRDaGFuZ2VzVG9ETVAiLCJjb252ZXJ0Q2hhbmdlc1RvWE1MIiwiY2Fub25pY2FsaXplIl0sIm1hcHBpbmdzIjoiOzs7Ozt1QkFnQkE7Ozs7dUJBQ0E7O0FBQ0E7O0FBQ0E7O0FBQ0E7O0FBRUE7O0FBQ0E7O0FBRUE7O0FBRUE7O0FBQ0E7O0FBQ0E7O0FBQ0E7O0FBRUE7O0FBQ0E7Ozs7QUFqQ0E7O0FBRUE7Ozs7Ozs7Ozs7Ozs7O2dDQWtDRUEsSSxHQUFBQSxtSDt5REFFQUMsUyxHQUFBQSwrRjtnQ0FDQUMsUyxHQUFBQSwwRjtnQ0FDQUMsa0IsR0FBQUEsbUc7Z0NBQ0FDLFMsR0FBQUEsMEY7Z0NBQ0FDLGdCLEdBQUFBLGlHO2dDQUNBQyxhLEdBQUFBLGtHO2dDQUVBQyxPLEdBQUFBLHVGO2dDQUNBQyxRLEdBQUFBLHlGO2dDQUVBQyxVLEdBQUFBLDRGO2dDQUVBQyxlLEdBQUFBLGtHO2dDQUNBQyxtQixHQUFBQSxzRztnQ0FDQUMsVyxHQUFBQSw4RjtnQ0FDQUMsVSxHQUFBQSw0RjtnQ0FDQUMsWSxHQUFBQSw4RjtnQ0FDQUMsVSxHQUFBQSw0RjtnQ0FDQUMsSyxHQUFBQSx1RjtnQ0FDQUMsbUIsR0FBQUEsbUc7Z0NBQ0FDLG1CLEdBQUFBLG1HO2dDQUNBQyxZLEdBQUFBLDZGIiwiZmlsZSI6ImluZGV4LmpzIiwic291cmNlc0NvbnRlbnQiOlsiLyogU2VlIExJQ0VOU0UgZmlsZSBmb3IgdGVybXMgb2YgdXNlICovXG5cbi8qXG4gKiBUZXh0IGRpZmYgaW1wbGVtZW50YXRpb24uXG4gKlxuICogVGhpcyBsaWJyYXJ5IHN1cHBvcnRzIHRoZSBmb2xsb3dpbmcgQVBJUzpcbiAqIEpzRGlmZi5kaWZmQ2hhcnM6IENoYXJhY3RlciBieSBjaGFyYWN0ZXIgZGlmZlxuICogSnNEaWZmLmRpZmZXb3JkczogV29yZCAoYXMgZGVmaW5lZCBieSBcXGIgcmVnZXgpIGRpZmYgd2hpY2ggaWdub3JlcyB3aGl0ZXNwYWNlXG4gKiBKc0RpZmYuZGlmZkxpbmVzOiBMaW5lIGJhc2VkIGRpZmZcbiAqXG4gKiBKc0RpZmYuZGlmZkNzczogRGlmZiB0YXJnZXRlZCBhdCBDU1MgY29udGVudFxuICpcbiAqIFRoZXNlIG1ldGhvZHMgYXJlIGJhc2VkIG9uIHRoZSBpbXBsZW1lbnRhdGlvbiBwcm9wb3NlZCBpblxuICogXCJBbiBPKE5EKSBEaWZmZXJlbmNlIEFsZ29yaXRobSBhbmQgaXRzIFZhcmlhdGlvbnNcIiAoTXllcnMsIDE5ODYpLlxuICogaHR0cDovL2NpdGVzZWVyeC5pc3QucHN1LmVkdS92aWV3ZG9jL3N1bW1hcnk/ZG9pPTEwLjEuMS40LjY5MjdcbiAqL1xuaW1wb3J0IERpZmYgZnJvbSAnLi9kaWZmL2Jhc2UnO1xuaW1wb3J0IHtkaWZmQ2hhcnN9IGZyb20gJy4vZGlmZi9jaGFyYWN0ZXInO1xuaW1wb3J0IHtkaWZmV29yZHMsIGRpZmZXb3Jkc1dpdGhTcGFjZX0gZnJvbSAnLi9kaWZmL3dvcmQnO1xuaW1wb3J0IHtkaWZmTGluZXMsIGRpZmZUcmltbWVkTGluZXN9IGZyb20gJy4vZGlmZi9saW5lJztcbmltcG9ydCB7ZGlmZlNlbnRlbmNlc30gZnJvbSAnLi9kaWZmL3NlbnRlbmNlJztcblxuaW1wb3J0IHtkaWZmQ3NzfSBmcm9tICcuL2RpZmYvY3NzJztcbmltcG9ydCB7ZGlmZkpzb24sIGNhbm9uaWNhbGl6ZX0gZnJvbSAnLi9kaWZmL2pzb24nO1xuXG5pbXBvcnQge2RpZmZBcnJheXN9IGZyb20gJy4vZGlmZi9hcnJheSc7XG5cbmltcG9ydCB7YXBwbHlQYXRjaCwgYXBwbHlQYXRjaGVzfSBmcm9tICcuL3BhdGNoL2FwcGx5JztcbmltcG9ydCB7cGFyc2VQYXRjaH0gZnJvbSAnLi9wYXRjaC9wYXJzZSc7XG5pbXBvcnQge21lcmdlfSBmcm9tICcuL3BhdGNoL21lcmdlJztcbmltcG9ydCB7c3RydWN0dXJlZFBhdGNoLCBjcmVhdGVUd29GaWxlc1BhdGNoLCBjcmVhdGVQYXRjaH0gZnJvbSAnLi9wYXRjaC9jcmVhdGUnO1xuXG5pbXBvcnQge2NvbnZlcnRDaGFuZ2VzVG9ETVB9IGZyb20gJy4vY29udmVydC9kbXAnO1xuaW1wb3J0IHtjb252ZXJ0Q2hhbmdlc1RvWE1MfSBmcm9tICcuL2NvbnZlcnQveG1sJztcblxuZXhwb3J0IHtcbiAgRGlmZixcblxuICBkaWZmQ2hhcnMsXG4gIGRpZmZXb3JkcyxcbiAgZGlmZldvcmRzV2l0aFNwYWNlLFxuICBkaWZmTGluZXMsXG4gIGRpZmZUcmltbWVkTGluZXMsXG4gIGRpZmZTZW50ZW5jZXMsXG5cbiAgZGlmZkNzcyxcbiAgZGlmZkpzb24sXG5cbiAgZGlmZkFycmF5cyxcblxuICBzdHJ1Y3R1cmVkUGF0Y2gsXG4gIGNyZWF0ZVR3b0ZpbGVzUGF0Y2gsXG4gIGNyZWF0ZVBhdGNoLFxuICBhcHBseVBhdGNoLFxuICBhcHBseVBhdGNoZXMsXG4gIHBhcnNlUGF0Y2gsXG4gIG1lcmdlLFxuICBjb252ZXJ0Q2hhbmdlc1RvRE1QLFxuICBjb252ZXJ0Q2hhbmdlc1RvWE1MLFxuICBjYW5vbmljYWxpemVcbn07XG4iXX0=
