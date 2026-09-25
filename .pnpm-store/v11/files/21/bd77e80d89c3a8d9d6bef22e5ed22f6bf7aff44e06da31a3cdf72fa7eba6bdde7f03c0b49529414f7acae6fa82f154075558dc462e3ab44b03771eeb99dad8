# powershell-utils

> Utilities for executing PowerShell commands

## Install

```sh
npm install powershell-utils
```

## Usage

```js
import {executePowerShell, powerShellPath} from 'powershell-utils';

const {stdout} = await executePowerShell('Get-Process');
console.log(stdout);

console.log(powerShellPath());
//=> 'C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe'
```

## API

### powerShellPath()

Returns: `string`

Get the PowerShell executable path on Windows.

```js
import {powerShellPath} from 'powershell-utils';

const psPath = powerShellPath();
//=> 'C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe'
```

### canAccessPowerShell()

Returns: `Promise<boolean>`

Check if PowerShell is accessible on Windows.

This checks if the PowerShell executable exists and has execute permissions. Useful for detecting restricted environments where PowerShell may be disabled by administrators.

```js
import {canAccessPowerShell} from 'powershell-utils';

if (await canAccessPowerShell()) {
	console.log('PowerShell is available');
} else {
	console.log('PowerShell is not accessible');
}
```

### executePowerShell(command, options?)

Returns: `Promise<{stdout: string, stderr: string}>`

Execute a PowerShell command.

```js
import {executePowerShell} from 'powershell-utils';

const {stdout} = await executePowerShell('Get-Process');
console.log(stdout);
```

#### command

Type: `string`

The PowerShell command to execute.

#### options

Type: `object`

The below option and all options except `shell` in Node.js [`child_process.execFile()`](https://nodejs.org/api/child_process.html#child_processexecfilefile-args-options-callback) are supported. PowerShell is always executed directly to prevent shell injection.

##### powerShellPath

Type: `string`\
Default: `powerShellPath()`

Path to PowerShell executable. Useful when calling from WSL or when PowerShell is in a non-standard location.

##### encoding

Type: `string`\
Default: `'utf8'`

Character encoding for stdout and stderr.

### executePowerShell.argumentsPrefix

Type: `string[]`

Standard PowerShell arguments that prefix the encoded command: `['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-EncodedCommand']`

Exposed for debugging or for advanced use cases where you need to customize the arguments. For most cases, use `createArguments()` instead.

### executePowerShell.encodeCommand(command)

Returns: `string`

Encode a PowerShell command as Base64 UTF-16LE.

This encoding prevents shell escaping issues and ensures complex commands with special characters are executed reliably.

#### command

Type: `string`

The PowerShell command to encode.

```js
import {executePowerShell} from 'powershell-utils';

const encoded = executePowerShell.encodeCommand('Get-Process');
```

### executePowerShell.escapeArgument(value)

Returns: `string`

Escape a string argument for use in PowerShell single-quoted strings.

> [!IMPORTANT]
> This makes a value safe as a PowerShell string literal, not as an argument to a native program. PowerShell removes the outer quotes before passing a value onwards, and the receiving process then re-splits it under its own command-line rules, where spaces and double quotes are delimiters. For example, `Start-Process -ArgumentList` receives `'a b'` as two arguments rather than one.
>
> Escaping also does not make an untrusted value safe to use as the program that `Start-Process` runs. Validate such values against an allowlist.

#### value

Type: `unknown`

The value to escape.

```js
import {executePowerShell} from 'powershell-utils';

const escaped = executePowerShell.escapeArgument("it's a test");
//=> "'it''s a test'"

// Use in command building
const command = `Start-Process ${executePowerShell.escapeArgument(appName)}`;
```

### executePowerShell.createArguments(command)

Returns: `string[]`

Create the full arguments array for PowerShell execution.

Combines `argumentsPrefix` with the encoded command. Useful when using `spawn()`, `execFile()`, or other process execution methods.

#### command

Type: `string`

The PowerShell command.

```js
import {spawn} from 'node:child_process';
import {powerShellPath, executePowerShell} from 'powershell-utils';

const args = executePowerShell.createArguments('Get-Process');
spawn(powerShellPath(), args);
```

### executePowerShellSync(command, options?)

Returns: `string`

Execute a PowerShell command synchronously.

```js
import {executePowerShellSync} from 'powershell-utils';

const stdout = executePowerShellSync('Get-Process');
console.log(stdout);
```

#### command

Type: `string`

The PowerShell command to execute.

#### options

Type: `object`

The below option and all options except `shell` in Node.js [`child_process.execFileSync()`](https://nodejs.org/api/child_process.html#child_processexecfilesyncfile-args-options) are supported. PowerShell is always executed directly to prevent shell injection.

##### powerShellPath

Type: `string`\
Default: `powerShellPath()`

Path to PowerShell executable.

##### encoding

Type: `string`\
Default: `'utf8'`

Character encoding for the output.
