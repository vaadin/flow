/*istanbul ignore start*/'use strict';

exports.__esModule = true;
exports. /*istanbul ignore end*/parsePatch = parsePatch;
function parsePatch(uniDiff) {
  /*istanbul ignore start*/var /*istanbul ignore end*/options = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

  var diffstr = uniDiff.split(/\r\n|[\n\v\f\r\x85]/),
      delimiters = uniDiff.match(/\r\n|[\n\v\f\r\x85]/g) || [],
      list = [],
      i = 0;

  function parseIndex() {
    var index = {};
    list.push(index);

    // Parse diff metadata
    while (i < diffstr.length) {
      var line = diffstr[i];

      // File header found, end parsing diff metadata
      if (/^(\-\-\-|\+\+\+|@@)\s/.test(line)) {
        break;
      }

      // Diff index
      var headerMatch = /^(?:Index:|diff(?: -r \w+)+)\s+/.exec(line);
      if (headerMatch) {
        index.index = line.substring(headerMatch[0].length).trim();
      }

      i++;
    }

    // Parse file headers if they are defined. Unified diff requires them, but
    // there's no technical issues to have an isolated hunk without file header
    parseFileHeader(index);
    parseFileHeader(index);

    // Parse hunks
    index.hunks = [];

    while (i < diffstr.length) {
      var _line = diffstr[i];

      if (/^(Index:|diff|\-\-\-|\+\+\+)\s/.test(_line)) {
        break;
      } else if (/^@@/.test(_line)) {
        index.hunks.push(parseHunk());
      } else if (_line && options.strict) {
        // Ignore unexpected content unless in strict mode
        throw new Error('Unknown line ' + (i + 1) + ' ' + JSON.stringify(_line));
      } else {
        i++;
      }
    }
  }

  // Parses the --- and +++ headers, if none are found, no lines
  // are consumed.
  function parseFileHeader(index) {
    var fileHeaderMatch = /^(---|\+\+\+)\s+/.exec(diffstr[i]);
    if (fileHeaderMatch) {
      var keyPrefix = fileHeaderMatch[1] === '---' ? 'old' : 'new';
      var data = diffstr[i].substring(3).trim().split('\t', 2);
      var fileName = data[0].replace(/\\\\/g, '\\');
      if (fileName.startsWith('"') && fileName.endsWith('"')) {
        fileName = fileName.substr(1, fileName.length - 2);
      }
      index[keyPrefix + 'FileName'] = fileName;
      index[keyPrefix + 'Header'] = (data[1] || '').trim();

      i++;
    }
  }

  // Parses a hunk
  // This assumes that we are at the start of a hunk.
  function parseHunk() {
    var chunkHeaderIndex = i,
        chunkHeaderLine = diffstr[i++],
        chunkHeader = chunkHeaderLine.split(/@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/);

    var hunk = {
      oldStart: +chunkHeader[1],
      oldLines: +chunkHeader[2] || 1,
      newStart: +chunkHeader[3],
      newLines: +chunkHeader[4] || 1,
      lines: [],
      linedelimiters: []
    };

    var addCount = 0,
        removeCount = 0;
    for (; i < diffstr.length; i++) {
      // Lines starting with '---' could be mistaken for the "remove line" operation
      // But they could be the header for the next file. Therefore prune such cases out.
      if (diffstr[i].indexOf('--- ') === 0 && i + 2 < diffstr.length && diffstr[i + 1].indexOf('+++ ') === 0 && diffstr[i + 2].indexOf('@@') === 0) {
        break;
      }
      var operation = diffstr[i].length == 0 && i != diffstr.length - 1 ? ' ' : diffstr[i][0];

      if (operation === '+' || operation === '-' || operation === ' ' || operation === '\\') {
        hunk.lines.push(diffstr[i]);
        hunk.linedelimiters.push(delimiters[i] || '\n');

        if (operation === '+') {
          addCount++;
        } else if (operation === '-') {
          removeCount++;
        } else if (operation === ' ') {
          addCount++;
          removeCount++;
        }
      } else {
        break;
      }
    }

    // Handle the empty block count case
    if (!addCount && hunk.newLines === 1) {
      hunk.newLines = 0;
    }
    if (!removeCount && hunk.oldLines === 1) {
      hunk.oldLines = 0;
    }

    // Perform optional sanity checking
    if (options.strict) {
      if (addCount !== hunk.newLines) {
        throw new Error('Added line count did not match for hunk at line ' + (chunkHeaderIndex + 1));
      }
      if (removeCount !== hunk.oldLines) {
        throw new Error('Removed line count did not match for hunk at line ' + (chunkHeaderIndex + 1));
      }
    }

    return hunk;
  }

  while (i < diffstr.length) {
    parseIndex();
  }

  return list;
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9wYXRjaC9wYXJzZS5qcyJdLCJuYW1lcyI6WyJwYXJzZVBhdGNoIiwidW5pRGlmZiIsIm9wdGlvbnMiLCJkaWZmc3RyIiwic3BsaXQiLCJkZWxpbWl0ZXJzIiwibWF0Y2giLCJsaXN0IiwiaSIsInBhcnNlSW5kZXgiLCJpbmRleCIsInB1c2giLCJsZW5ndGgiLCJsaW5lIiwidGVzdCIsImhlYWRlck1hdGNoIiwiZXhlYyIsInN1YnN0cmluZyIsInRyaW0iLCJwYXJzZUZpbGVIZWFkZXIiLCJodW5rcyIsInBhcnNlSHVuayIsInN0cmljdCIsIkVycm9yIiwiSlNPTiIsInN0cmluZ2lmeSIsImZpbGVIZWFkZXJNYXRjaCIsImtleVByZWZpeCIsImRhdGEiLCJmaWxlTmFtZSIsInJlcGxhY2UiLCJzdGFydHNXaXRoIiwiZW5kc1dpdGgiLCJzdWJzdHIiLCJjaHVua0hlYWRlckluZGV4IiwiY2h1bmtIZWFkZXJMaW5lIiwiY2h1bmtIZWFkZXIiLCJodW5rIiwib2xkU3RhcnQiLCJvbGRMaW5lcyIsIm5ld1N0YXJ0IiwibmV3TGluZXMiLCJsaW5lcyIsImxpbmVkZWxpbWl0ZXJzIiwiYWRkQ291bnQiLCJyZW1vdmVDb3VudCIsImluZGV4T2YiLCJvcGVyYXRpb24iXSwibWFwcGluZ3MiOiI7OztnQ0FBZ0JBLFUsR0FBQUEsVTtBQUFULFNBQVNBLFVBQVQsQ0FBb0JDLE9BQXBCLEVBQTJDO0FBQUEsc0RBQWRDLE9BQWMsdUVBQUosRUFBSTs7QUFDaEQsTUFBSUMsVUFBVUYsUUFBUUcsS0FBUixDQUFjLHFCQUFkLENBQWQ7QUFBQSxNQUNJQyxhQUFhSixRQUFRSyxLQUFSLENBQWMsc0JBQWQsS0FBeUMsRUFEMUQ7QUFBQSxNQUVJQyxPQUFPLEVBRlg7QUFBQSxNQUdJQyxJQUFJLENBSFI7O0FBS0EsV0FBU0MsVUFBVCxHQUFzQjtBQUNwQixRQUFJQyxRQUFRLEVBQVo7QUFDQUgsU0FBS0ksSUFBTCxDQUFVRCxLQUFWOztBQUVBO0FBQ0EsV0FBT0Ys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LENBQW5DLEVBQXNDO0FBQ3BDSixXQUFLSSxRQUFMLEdBQWdCLENBQWhCO0FBQ0Q7QUFDRCxRQUFJLENBQUNJLFdBQUQsSUFBZ0JSLEtBQUtFLFFBQUwsS0FBa0IsQ0FBdEMsRUFBeUM7QUFDdkNGLFdBQUtFLFFBQUwsR0FBZ0IsQ0FBaEI7QUFDRDs7QUFFRDtBQUNBLFFBQUlyQyxRQUFRb0IsTUFBWixFQUFvQjtBQUNsQixVQUFJc0IsYUFBYVAsS0FBS0ksUUFBdEIsRUFBZ0M7QUFDOUIsY0FBTSxJQUFJbEIsS0FBSixDQUFVLHNEQUFzRFcsbUJBQW1CLENBQXpFLENBQVYsQ0FBTjtBQUNEO0FBQ0QsVUFBSVcsZ0JBQWdCUixLQUFLRSxRQUF6QixFQUFtQztBQUNqQyxjQUFNLElBQUloQixLQUFKLENBQVUsd0RBQXdEVyxtQkFBbUIsQ0FBM0UsQ0FBVixDQUFOO0FBQ0Q7QUFDRjs7QUFFRCxXQUFPRyxJQUFQO0FBQ0Q7O0FBRUQsU0FBTzdCLElBQUlMLFFBQVFTLE1BQW5CLEVBQTJCO0FBQ3pCSDtBQUNEOztBQUVELFNBQU9GLElBQVA7QUFDRCIsImZpbGUiOiJwYXJzZS5qcyIsInNvdXJjZXNDb250ZW50IjpbImV4cG9ydCBmdW5jdGlvbiBwYXJzZVBhdGNoKHVuaURpZmYsIG9wdGlvbnMgPSB7fSkge1xuICBsZXQgZGlmZnN0ciA9IHVuaURpZmYuc3BsaXQoL1xcclxcbnxbXFxuXFx2XFxmXFxyXFx4ODVdLyksXG4gICAgICBkZWxpbWl0ZXJzID0gdW5pRGlmZi5tYXRjaCgvXFxyXFxufFtcXG5cXHZcXGZcXHJcXHg4NV0vZykgfHwgW10sXG4gICAgICBsaXN0ID0gW10sXG4gICAgICBpID0gMDtcblxuICBmdW5jdGlvbiBwYXJzZUluZGV4KCkge1xuICAgIGxldCBpbmRleCA9IHt9O1xuICAgIGxpc3QucHVzaChpbmRleCk7XG5cbiAgICAvLyBQYXJzZSBkaWZmIG1ldGFkYXRhXG4gICAgd2hpbGUgKGkgPCBkaWZmc3RyLmxlbmd0aCkge1xuICAgICAgbGV0IGxpbmUgPSBkaWZmc3RyW2ldO1xuXG4gICAgICAvLyBGaWxlIGhlYWRlciBmb3VuZCwgZW5kIHBhcnNpbmcgZGlmZiBtZXRhZGF0YVxuICAgICAgaWYgKC9eKFxcLVxcLVxcLXxcXCtcXCtcXCt8QEApXFxzLy50ZXN0KGxpbmUpKSB7XG4gICAgICAgIGJyZWFrO1xuICAgICAgfVxuXG4gICAgICAvLyBEaWZmIGluZGV4XG4gICAgICBsZXQgaGVhZGVyTWF0Y2ggPSAoL14oPzpJbmRleDp8ZGlmZig/OiAtciBcXHcrKSspXFxzKy8pLmV4ZWMobGluZSk7XG4gICAgICBpZiAoaGVhZGVyTWF0Y2gpIHtcbiAgICAgICAgaW5kZXguaW5kZXggPSBsaW5lLnN1YnN0cmluZyhoZWFkZXJNYXRjaFswXS5sZW5ndGgpLnRyaW0oKTtcbiAgICAgIH1cblxuICAgICAgaSsrO1xuICAgIH1cblxuICAgIC8vIFBhcnNlIGZpbGUgaGVhZGVycyBpZiB0aGV5IGFyZSBkZWZpbmVkLiBVbmlmaWVkIGRpZmYgcmVxdWlyZXMgdGhlbSwgYnV0XG4gICAgLy8gdGhlcmUncyBubyB0ZWNobmljYWwgaXNzdWVzIHRvIGhhdmUgYW4gaXNvbGF0ZWQgaHVuayB3aXRob3V0IGZpbGUgaGVhZGVyXG4gICAgcGFyc2VGaWxlSGVhZGVyKGluZGV4KTtcbiAgICBwYXJzZUZpbGVIZWFkZXIoaW5kZXgpO1xuXG4gICAgLy8gUGFyc2UgaHVua3NcbiAgICBpbmRleC5odW5rcyA9IFtdO1xuXG4gICAgd2hpbGUgKGkgPCBkaWZmc3RyLmxlbmd0aCkge1xuICAgICAgbGV0IGxpbmUgPSBkaWZmc3RyW2ldO1xuXG4gICAgICBpZiAoL14oSW5kZXg6fGRpZmZ8XFwtXFwtXFwtfFxcK1xcK1xcKylcXHMvLnRlc3QobGluZSkpIHtcbiAgICAgICAgYnJlYWs7XG4gICAgICB9IGVsc2UgaWYgKC9eQEAvLnRlc3QobGluZSkpIHtcbiAgICAgICAgaW5kZXguaHVua3MucHVzaChwYXJzZUh1bmsoKSk7XG4gICAgICB9IGVsc2UgaWYgKGxpbmUgJiYgb3B0aW9ucy5zdHJpY3QpIHtcbiAgICAgICAgLy8gSWdub3JlIHVuZXhwZWN0ZWQgY29udGVudCB1bmxlc3MgaW4gc3RyaWN0IG1vZGVcbiAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdVbmtub3duIGxpbmUgJyArIChpICsgMSkgKyAnICcgKyBKU09OLnN0cmluZ2lmeShsaW5lKSk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBpKys7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgLy8gUGFyc2VzIHRoZSAtLS0gYW5kICsrKyBoZWFkZXJzLCBpZiBub25lIGFyZSBmb3VuZCwgbm8gbGluZXNcbiAgLy8gYXJlIGNvbnN1bWVkLlxuICBmdW5jdGlvbiBwYXJzZUZpbGVIZWFkZXIoaW5kZXgpIHtcbiAgICBjb25zdCBmaWxlSGVhZGVyTWF0Y2ggPSAoL14oLS0tfFxcK1xcK1xcKylcXHMrLykuZXhlYyhkaWZmc3RyW2ldKTtcbiAgICBpZiAoZmlsZUhlYWRlck1hdGNoKSB7XG4gICAgICBsZXQga2V5UHJlZml4ID0gZmlsZUhlYWRlck1hdGNoWzFdID09PSAnLS0tJyA/ICdvbGQnIDogJ25ldyc7XG4gICAgICBjb25zdCBkYXRhID0gZGlmZnN0cltpXS5zdWJzdHJpbmcoMykudHJpbSgpLnNwbGl0KCdcXHQnLCAyKTtcbiAgICAgIGxldCBmaWxlTmFtZSA9IGRhdGFbMF0ucmVwbGFjZSgvXFxcXFxcXFwvZywgJ1xcXFwnKTtcbiAgICAgIGlmIChmaWxlTmFtZS5zdGFydHNXaXRoKCdcIicpICYmIGZpbGVOYW1lLmVuZHNXaXRoKCdcIicpKSB7XG4gICAgICAgIGZpbGVOYW1lID0gZmlsZU5hbWUuc3Vic3RyKDEsIGZpbGVOYW1lLmxlbmd0aCAtIDIpO1xuICAgICAgfVxuICAgICAgaW5kZXhba2V5UHJlZml4ICsgJ0ZpbGVOYW1lJ10gPSBmaWxlTmFtZTtcbiAgICAgIGluZGV4W2tleVByZWZpeCArICdIZWFkZXInXSA9IChkYXRhWzFdIHx8ICcnKS50cmltKCk7XG5cbiAgICAgIGkrKztcbiAgICB9XG4gIH1cblxuICAvLyBQYXJzZXMgYSBodW5rXG4gIC8vIFRoaXMgYXNzdW1lcyB0aGF0IHdlIGFyZSBhdCB0aGUgc3RhcnQgb2YgYSBodW5rLlxuICBmdW5jdGlvbiBwYXJzZUh1bmsoKSB7XG4gICAgbGV0IGNodW5rSGVhZGVySW5kZXggPSBpLFxuICAgICAgICBjaHVua0hlYWRlckxpbmUgPSBkaWZmc3RyW2krK10sXG4gICAgICAgIGNodW5rSGVhZGVyID0gY2h1bmtIZWFkZXJMaW5lLnNwbGl0KC9AQCAtKFxcZCspKD86LChcXGQrKSk/IFxcKyhcXGQrKSg/OiwoXFxkKykpPyBAQC8pO1xuXG4gICAgbGV0IGh1bmsgPSB7XG4gICAgICBvbGRTdGFydDogK2NodW5rSGVhZGVyWzFdLFxuICAgICAgb2xkTGluZXM6ICtjaHVua0hlYWRlclsyXSB8fCAxLFxuICAgICAgbmV3U3RhcnQ6ICtjaHVua0hlYWRlclszXSxcbiAgICAgIG5ld0xpbmVzOiArY2h1bmtIZWFkZXJbNF0gfHwgMSxcbiAgICAgIGxpbmVzOiBbXSxcbiAgICAgIGxpbmVkZWxpbWl0ZXJzOiBbXVxuICAgIH07XG5cbiAgICBsZXQgYWRkQ291bnQgPSAwLFxuICAgICAgICByZW1vdmVDb3VudCA9IDA7XG4gICAgZm9yICg7IGkgPCBkaWZmc3RyLmxlbmd0aDsgaSsrKSB7XG4gICAgICAvLyBMaW5lcyBzdGFydGluZyB3aXRoICctLS0nIGNvdWxkIGJlIG1pc3Rha2VuIGZvciB0aGUgXCJyZW1vdmUgbGluZVwiIG9wZXJhdGlvblxuICAgICAgLy8gQnV0IHRoZXkgY291bGQgYmUgdGhlIGhlYWRlciBmb3IgdGhlIG5leHQgZmlsZS4gVGhlcmVmb3JlIHBydW5lIHN1Y2ggY2FzZXMgb3V0LlxuICAgICAgaWYgKGRpZmZzdHJbaV0uaW5kZXhPZignLS0tICcpID09PSAwXG4gICAgICAgICAgICAmJiAoaSArIDIgPCBkaWZmc3RyLmxlbmd0aClcbiAgICAgICAgICAgICYmIGRpZmZzdHJbaSArIDFdLmluZGV4T2YoJysrKyAnKSA9PT0gMFxuICAgICAgICAgICAgJiYgZGlmZnN0cltpICsgMl0uaW5kZXhPZignQEAnKSA9PT0gMCkge1xuICAgICAgICAgIGJyZWFrO1xuICAgICAgfVxuICAgICAgbGV0IG9wZXJhdGlvbiA9IChkaWZmc3RyW2ldLmxlbmd0aCA9PSAwICYmIGkgIT0gKGRpZmZzdHIubGVuZ3RoIC0gMSkpID8gJyAnIDogZGlmZnN0cltpXVswXTtcblxuICAgICAgaWYgKG9wZXJhdGlvbiA9PT0gJysnIHx8IG9wZXJhdGlvbiA9PT0gJy0nIHx8IG9wZXJhdGlvbiA9PT0gJyAnIHx8IG9wZXJhdGlvbiA9PT0gJ1xcXFwnKSB7XG4gICAgICAgIGh1bmsubGluZXMucHVzaChkaWZmc3RyW2ldKTtcbiAgICAgICAgaHVuay5saW5lZGVsaW1pdGVycy5wdXNoKGRlbGltaXRlcnNbaV0gfHwgJ1xcbicpO1xuXG4gICAgICAgIGlmIChvcGVyYXRpb24gPT09ICcrJykge1xuICAgICAgICAgIGFkZENvdW50Kys7XG4gICAgICAgIH0gZWxzZSBpZiAob3BlcmF0aW9uID09PSAnLScpIHtcbiAgICAgICAgICByZW1vdmVDb3VudCsrO1xuICAgICAgICB9IGVsc2UgaWYgKG9wZXJhdGlvbiA9PT0gJyAnKSB7XG4gICAgICAgICAgYWRkQ291bnQrKztcbiAgICAgICAgICByZW1vdmVDb3VudCsrO1xuICAgICAgICB9XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBicmVhaztcbiAgICAgIH1cbiAgICB9XG5cbiAgICAvLyBIYW5kbGUgdGhlIGVtcHR5IGJsb2NrIGNvdW50IGNhc2VcbiAgICBpZiAoIWFkZENvdW50ICYmIGh1bmsubmV3TGluZXMgPT09IDEpIHtcbiAgICAgIGh1bmsubmV3TGluZXMgPSAwO1xuICAgIH1cbiAgICBpZiAoIXJlbW92ZUNvdW50ICYmIGh1bmsub2xkTGluZXMgPT09IDEpIHtcbiAgICAgIGh1bmsub2xkTGluZXMgPSAwO1xuICAgIH1cblxuICAgIC8vIFBlcmZvcm0gb3B0aW9uYWwgc2FuaXR5IGNoZWNraW5nXG4gICAgaWYgKG9wdGlvbnMuc3RyaWN0KSB7XG4gICAgICBpZiAoYWRkQ291bnQgIT09IGh1bmsubmV3TGluZXMpIHtcbiAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdBZGRlZCBsaW5lIGNvdW50IGRpZCBub3QgbWF0Y2ggZm9yIGh1bmsgYXQgbGluZSAnICsgKGNodW5rSGVhZGVySW5kZXggKyAxKSk7XG4gICAgICB9XG4gICAgICBpZiAocmVtb3ZlQ291bnQgIT09IGh1bmsub2xkTGluZXMpIHtcbiAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdSZW1vdmVkIGxpbmUgY291bnQgZGlkIG5vdCBtYXRjaCBmb3IgaHVuayBhdCBsaW5lICcgKyAoY2h1bmtIZWFkZXJJbmRleCArIDEpKTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gaHVuaztcbiAgfVxuXG4gIHdoaWxlIChpIDwgZGlmZnN0ci5sZW5ndGgpIHtcbiAgICBwYXJzZUluZGV4KCk7XG4gIH1cblxuICByZXR1cm4gbGlzdDtcbn1cbiJdfQ==
