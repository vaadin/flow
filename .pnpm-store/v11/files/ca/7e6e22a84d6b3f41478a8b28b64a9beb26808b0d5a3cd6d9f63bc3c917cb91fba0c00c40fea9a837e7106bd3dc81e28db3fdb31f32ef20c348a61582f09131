[![Build Status](https://travis-ci.org/Polymer/polyserve.svg?branch=master)](https://travis-ci.org/Polymer/polyserve)
[![NPM version](http://img.shields.io/npm/v/polyserve.svg)](https://www.npmjs.com/package/polyserve)

# polyserve

A simple development server for web projects.

`polyserve` serves project files from a URL root that allows relative URLs
that reach out of the project, like those starting with `../`, to work. This is
necessary for referencing other packages by path when stored as a flat directory
such as how Bower works.

The local package is served at the URL `/components/{bower-name}/`, with files
served from the current directory. Other packages are served at
`/components/{packageName}` with files served from their directory under
`./bower_components/`.

## Installation

    $ npm install polyserve -g

## Usage

### Run `polyserve`

    $ cd my-element/
    $ polyserve

### Browse files

Navigate to `localhost:8080/components/my-element/demo.html`

### Options

 * `--version`: Print version info.                                                           
 * `--root` _string_: The root directory of your project. Defaults to the current working directory.                                                                    
 * `--compile` _string_: Compiler options. Valid values are "auto", "always" and "never". "auto" compiles JavaScript to ES5 for browsers that don't fully support ES6.         
 * `--module-resolution` _string_: Algorithm to use for resolving module specifiers in import and export statements when rewriting them to be web-compatible. Valid values are "none" and "node". "none" disables module specifier rewriting. "node" uses Node.js resolution to find modules.
 * `--compile-cache` _number_: Maximum size in bytes (actually, UTF-8 characters) of the cache used to store results for JavaScript compilation. Cache size includes the uncompiled and compiled file content lengths. Defaults to 52428800 (50MB).
 * `-p`, `--port` _number_: The port to serve from. Serve will choose an open port for you by default.
 * `-H`, `--hostname` _string_: The hostname to serve from. Defaults to localhost.          
 * `-c`, `--component-dir` _string_: The component directory to use. Defaults to reading from the Bower config (usually bower_components/).
 * `-u`, `--component-url` _string_: The component url to use. Defaults to reading from the Bower config (usually bower_components/).
 * `-n`, `--package-name` _string_: The package name to use for the root directory. Defaults to reading from bower.json.
 * `--npm`: Sets npm mode: component directory is "node_modules" and the package name is read from package.json
 * `-o`, `--open`: The page to open in the default browser on startup.
 * `-b`, `--browser` _string[]_: The browser(s) to open with when using the --open option. Defaults to your default web browser.
 * `--open-path` _string_: The URL path to open when using the --open option. Defaults to "index.html".
 * `-P`, `--protocol` _string_: The server protocol to use {h2, https/1.1, http/1.1}. Defaults to "http/1.1".
 * `--key` _string_: Path to TLS certificate private key file for https. Defaults to "key.pem".
 * `--cert` _string_: Path to TLS certificate file for https. Defaults to "cert.pem".
 * `--manifest` _string_: Path to HTTP/2 Push Manifest.    
 * `--proxy-path` _string_: Top-level path that should be redirected to the proxy-target. E.g. `api/v1` when you want to redirect all requests of `https://localhost/api/v1/`.
 * `--proxy-target` _string_: Host URL to proxy to, for example `https://myredirect:8080/foo`.
 * `--help`: Shows this help message

## Compiling from Source

    $ npm install
    $ npm run build

You can compile and run polyserve from source by cloning the repo from Github and then running `npm run build`. Make sure you have already run `npm install` before building.

### Run Tests

    $ npm test
