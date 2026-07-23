// Pre-compresses static resources served straight from META-INF/resources
// (e.g. @StyleSheet CSS) into brotli (.br) and gzip (.gz) siblings, mirroring
// the compression Vite applies to bundled assets. Invoked with the resource
// root directory as the single argument. Uses only Node built-ins so the Flow
// build needs no additional dependency.
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { brotliCompressSync, constants, gzipSync } from 'node:zlib';

// File types that benefit from text compression.
const COMPRESSIBLE_EXTENSIONS = [
  '.css',
  '.js',
  '.mjs',
  '.cjs',
  '.json',
  '.map',
  '.svg',
  '.html',
  '.htm',
  '.xml',
  '.txt'
];

// Below this size the compressed variant tends to be as large as, or larger
// than, the original, so serving precompressed brings no benefit.
const MIN_SIZE_BYTES = 1024;

function isCompressible(fileName) {
  const lower = fileName.toLowerCase();
  return COMPRESSIBLE_EXTENSIONS.some((extension) => lower.endsWith(extension));
}

function collectFiles(directory, collected) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const fullPath = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      collectFiles(fullPath, collected);
    } else if (entry.isFile() && isCompressible(entry.name)) {
      collected.push(fullPath);
    }
  }
}

const root = process.argv[2];
if (!root || !existsSync(root)) {
  process.exit(0);
}

const files = [];
collectFiles(resolve(root), files);

for (const file of files) {
  const size = statSync(file).size;
  if (size < MIN_SIZE_BYTES) {
    continue;
  }
  const contents = readFileSync(file);
  writeFileSync(
    `${file}.br`,
    brotliCompressSync(contents, {
      params: {
        [constants.BROTLI_PARAM_QUALITY]: constants.BROTLI_MAX_QUALITY,
        [constants.BROTLI_PARAM_SIZE_HINT]: size
      }
    })
  );
  writeFileSync(`${file}.gz`, gzipSync(contents, { level: 9 }));
}
