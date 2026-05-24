#!/usr/bin/env python3
"""Extract Javadoc errors from a Maven log and emit a JUnit XML report.

Usage: javadocErrorsToJUnit.py <maven-log> <output-xml>

Scans the Maven log for Javadoc error blocks (header + source line +
caret, each prefixed by Maven's [ERROR] marker) and writes a JUnit XML
file with one testcase per error. The publish-unit-test-result-action
in the test-results job then surfaces them in the PR comment and job
summary alongside surefire/failsafe failures. Writes nothing if no
Javadoc errors are present in the log.
"""

import os
import re
import sys
from xml.sax.saxutils import escape, quoteattr

PATTERN = re.compile(
    r'^\[ERROR\] (\S+\.java):(\d+): error: (.*)$\n'
    r'^\[ERROR\] (.*)$\n'
    r'^\[ERROR\] (.*\^.*)$',
    re.M,
)


def relativize(path, workspace):
    if workspace and path.startswith(workspace + '/'):
        return path[len(workspace) + 1:]
    return path


def main(log_path, out_path):
    with open(log_path, encoding='utf-8', errors='replace') as f:
        log = f.read()
    workspace = os.environ.get('GITHUB_WORKSPACE', '')

    errors = PATTERN.findall(log)
    if not errors:
        print(f'No Javadoc errors found in {log_path}', file=sys.stderr)
        return 0

    parts = ['<?xml version="1.0" encoding="UTF-8"?>']
    n = len(errors)
    parts.append(
        f'<testsuite name="Javadoc" tests="{n}" failures="{n}" '
        f'errors="0" skipped="0">'
    )
    for path, line_no, msg, src, caret in errors:
        file_rel = relativize(path, workspace)
        name = f'{file_rel}:{line_no}'
        detail = f'{file_rel}:{line_no}: error: {msg}\n{src}\n{caret}'
        parts.append(
            f'  <testcase classname={quoteattr("Javadoc")} '
            f'name={quoteattr(name)}>'
        )
        parts.append(
            f'    <failure message={quoteattr(msg)}>'
            f'{escape(detail)}</failure>'
        )
        parts.append('  </testcase>')
    parts.append('</testsuite>')

    os.makedirs(os.path.dirname(out_path) or '.', exist_ok=True)
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(parts))
    print(f'Wrote {out_path} with {n} Javadoc failure(s)')
    return 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.stderr.write(__doc__ or '')
        sys.exit(2)
    sys.exit(main(sys.argv[1], sys.argv[2]))
