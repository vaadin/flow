#!/usr/bin/env node
// Parses Mermaid figures with the same grammar GitHub renders them with, so a
// bot that posts a diagram finds out about a syntax error before a reviewer
// does. Takes `.mmd` files, or `.md` files whose fenced ```mermaid blocks are
// extracted, or the figure on stdin. Exit 0 means every block parses.
//
//   node .github/scripts/validate-mermaid.mjs figure.mmd
//   node .github/scripts/validate-mermaid.mjs comment.md
//   cat figure.mmd | node .github/scripts/validate-mermaid.mjs -
//
// Mermaid is fetched from npm on first use and cached under the runner's temp
// directory. Without a network the script still applies the lint rules below,
// reports that the grammar check did not run, and exits 2.

import { execFileSync } from 'node:child_process';
import { createRequire } from 'node:module';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

// GitHub renders comments with Mermaid 11.x. jsdom is what lets Mermaid's
// sanitizer run outside a browser; without it every label fails to parse.
const PACKAGES = ['mermaid@^11', 'jsdom@^27'];
const CACHE = path.join(process.env.RUNNER_TEMP || os.tmpdir(), 'mermaid-validate');

// Each rule names a construct that the grammar accepts nowhere useful, or that
// renders into something other than what it says. `where` narrows a rule to the
// diagram types it applies to.
const RULES = [
    {
        id: 'unquoted-parens-in-label',
        where: /^(flowchart|graph)\b/,
        test: (line) => {
            const edge = line.match(/(?:--|-\.|==)[->.=]*\|([^|]*)\|/);
            const unquoted = (text) => /[()]/.test(text) && !/^\s*".*"\s*$/.test(text);
            return (edge && unquoted(edge[1]))
                || /[[({][^\]")}]*\w[^\]")}(]*\(/.test(line.replace(/"[^"]*"/g, '""'));
        },
        says: 'a label holding parentheses must be quoted: A["calls foo()"], -->|"calls foo()"|',
    },
    {
        id: 'backtick-opens-label',
        test: (line) => /[[({]\s*"`/.test(line),
        says: 'a backtick right after the quote starts a Mermaid markdown string; write ["@PWA offline"], not ["`@PWA` offline"]',
    },
    {
        id: 'semicolon-in-message-text',
        where: /^sequenceDiagram\b/,
        test: (line) => {
            const colon = line.indexOf(':');
            return colon !== -1 && line.slice(colon + 1).includes(';');
        },
        says: 'a semicolon ends the statement; use a comma or a second Note',
    },
    {
        id: 'quoted-message-text',
        where: /^sequenceDiagram\b/,
        test: (line) => /(?:->>|-->>|-x|--x)\s*[^:]*:\s*".*"\s*$/.test(line),
        says: 'message text is free text — the quotes would be drawn',
    },
    {
        id: 'init-directive',
        test: (line) => line.includes('%%{init'),
        says: 'an %%{init}%% block overrides the reader\'s own theme',
    },
    {
        id: 'raw-html',
        test: (line) => /<br\s*\/?>|<\/?(?:b|i|em|strong|span|div|font)\b/i.test(line),
        says: 'raw HTML does not survive GitHub\'s sanitizer',
    },
    {
        id: 'fill-without-color',
        test: (line) => /classDef\b/.test(line) && /\bfill:/.test(line) && !/\bcolor:/.test(line),
        says: 'a classDef that sets fill: must set color: too, or the dark theme keeps its near-white label',
    },
];

// The diagram type is the first line that is neither blank nor a comment.
function typeOf(text) {
    return text.split('\n').map((line) => line.trim())
        .find((line) => line && !line.startsWith('%%')) || '';
}

function lint(text, type) {
    const findings = [];
    text.split('\n').forEach((line, i) => {
        if (line.trim().startsWith('%%')) {
            return;
        }
        for (const rule of RULES) {
            if (rule.where && !rule.where.test(type)) {
                continue;
            }
            if (rule.test(line)) {
                findings.push({ line: i + 1, id: rule.id, says: rule.says, text: line.trim() });
            }
        }
    });
    return findings;
}

async function loadMermaid() {
    const require = createRequire(path.join(CACHE, 'noop.js'));
    for (const attempt of [0, 1]) {
        try {
            const { JSDOM } = await import(require.resolve('jsdom'));
            shim(new JSDOM('<!DOCTYPE html><body></body>').window);
            return (await import(require.resolve('mermaid'))).default;
        } catch (error) {
            if (attempt) {
                throw error;
            }
            fs.mkdirSync(CACHE, { recursive: true });
            execFileSync('npm', ['install', '--no-save', '--no-audit', '--no-fund',
                '--silent', '--prefix', CACHE, ...PACKAGES], { stdio: 'pipe' });
        }
    }
}

// Mermaid sanitizes every label through DOMPurify, which needs a document.
function shim(window) {
    globalThis.window = window;
    globalThis.document = window.document;
    Object.defineProperty(globalThis, 'navigator',
        { value: window.navigator, configurable: true });
    for (const name of ['Element', 'SVGElement', 'Node', 'HTMLElement', 'DOMParser',
        'XMLSerializer', 'MutationObserver', 'getComputedStyle']) {
        if (window[name] !== undefined) {
            globalThis[name] = window[name];
        }
    }
}

// The parse error names a line of the figure; quoting that line saves the
// reader from counting.
function quote(text, message) {
    const at = message.match(/^Parse error on line (\d+)|^Lexical error on line (\d+)/);
    if (!at) {
        return '';
    }
    const line = (text.split('\n')[Number(at[1] || at[2]) - 1] || '').trim();
    return line ? `\n       line ${at[1] || at[2]}: ${line}` : '';
}

function blocks(file) {
    const text = file === '-' ? fs.readFileSync(0, 'utf8') : fs.readFileSync(file, 'utf8');
    if (!file.endsWith('.md')) {
        return [{ name: file, text }];
    }
    const found = [...text.matchAll(/```+mermaid\s*\n([\s\S]*?)```+/g)];
    return found.map((match, i) => ({ name: `${file} block ${i + 1}`, text: match[1] }));
}

const files = process.argv.slice(2);
if (!files.length) {
    console.error('usage: validate-mermaid.mjs <file.mmd|file.md|-> ...');
    process.exit(64);
}

let mermaid;
let loadError;
try {
    mermaid = await loadMermaid();
    mermaid.initialize({ startOnLoad: false, securityLevel: 'loose' });
} catch (error) {
    loadError = error;
}

let failed = false;
let linted = 0;
for (const file of files) {
    for (const { name, text } of blocks(file)) {
        linted += 1;
        const problems = lint(text, typeOf(text)).map((f) => `  lint   ${name}: line ${f.line}, ${f.says}\n       ${f.text}`);
        let parse = null;
        if (mermaid) {
            try {
                await mermaid.parse(text);
            } catch (error) {
                const message = (error && (error.message || error.str)) || String(error);
                parse = `  PARSE  ${name}\n       ${message.split('\n').slice(0, 2).join(' ')}${quote(text, message)}`;
            }
        }
        if (parse) {
            failed = true;
            console.log(parse);
        }
        if (problems.length) {
            failed = true;
            console.log(problems.join('\n'));
        }
        if (!parse && !problems.length) {
            console.log(`  OK     ${name}`);
        }
    }
}

if (!linted) {
    console.log('no mermaid block found');
}
if (loadError) {
    console.log(`\nthe grammar check did not run: ${loadError.message.split('\n')[0]}`);
    console.log('only the lint rules above were applied — check the figure by hand');
    process.exit(2);
}
process.exit(failed ? 1 : 0);
