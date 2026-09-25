import type {ExecFileOptions, ExecFileSyncOptions} from 'node:child_process';

export type ExecutePowerShellOptions = Omit<ExecFileOptions, 'shell'> & {
	/**
	Path to PowerShell executable.

	@default powerShellPath()
	*/
	readonly powerShellPath?: string;

	/**
	Not supported. PowerShell is always executed directly to prevent shell injection.
	*/
	readonly shell?: never;
};

export type ExecutePowerShellSyncOptions = Omit<ExecFileSyncOptions, 'shell'> & {
	/**
	Path to PowerShell executable.

	@default powerShellPath()
	*/
	readonly powerShellPath?: string;

	/**
	Not supported. PowerShell is always executed directly to prevent shell injection.
	*/
	readonly shell?: never;
};

export type ExecutePowerShellResult = {
	readonly stdout: string;
	readonly stderr: string;
};

/**
Get the PowerShell executable path on Windows.

@returns The path to the PowerShell executable.

@example
```
import {powerShellPath} from 'powershell-utils';

const psPath = powerShellPath();
//=> 'C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe'
```
*/
export function powerShellPath(): string;

/**
Check if PowerShell is accessible on Windows.

This checks if the PowerShell executable exists and has execute permissions. Useful for detecting restricted environments where PowerShell may be disabled by administrators.

@returns A promise that resolves to true if PowerShell is accessible, false otherwise.

@example
```
import {canAccessPowerShell} from 'powershell-utils';

if (await canAccessPowerShell()) {
	console.log('PowerShell is available');
} else {
	console.log('PowerShell is not accessible');
}
```
*/
export function canAccessPowerShell(): Promise<boolean>;

/**
Execute a PowerShell command.

@param command - The PowerShell command to execute.
@returns A promise that resolves to the command output.

@example
```
import {executePowerShell} from 'powershell-utils';

const {stdout} = await executePowerShell('Get-Process');
console.log(stdout);
```
*/
export function executePowerShell(
	command: string,
	options?: ExecutePowerShellOptions
): Promise<ExecutePowerShellResult>;

export namespace executePowerShell {
	/**
	Standard PowerShell arguments that prefix the encoded command: `['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-EncodedCommand']`

	Exposed for debugging or for advanced use cases where you need to customize the arguments. For most cases, use `createArguments()` instead.
	*/
	export const argumentsPrefix: readonly string[];

	/**
	Encode a PowerShell command as Base64 UTF-16LE.

	This encoding prevents shell escaping issues and ensures complex commands with special characters are executed reliably.

	@param command - The PowerShell command to encode.
	@returns Base64-encoded command.

	@example
	```
	import {executePowerShell} from 'powershell-utils';

	const encoded = executePowerShell.encodeCommand('Get-Process');
	```
	*/
	export function encodeCommand(command: string): string;

	/**
	Escape a string argument for use in PowerShell single-quoted strings.

	This makes a value safe as a PowerShell string literal, not as an argument to a native program. PowerShell removes the outer quotes before passing a value onwards, and the receiving process then re-splits it under its own command-line rules, where spaces and double quotes are delimiters. For example, `Start-Process -ArgumentList` receives `'a b'` as two arguments rather than one.

	Escaping also does not make an untrusted value safe to use as the program that `Start-Process` runs. Validate such values against an allowlist.

	@param value - The value to escape.
	@returns Escaped and quoted string ready for PowerShell.

	@example
	```
	import {executePowerShell} from 'powershell-utils';

	const escaped = executePowerShell.escapeArgument("it's a test");
	//=> "'it''s a test'"

	// Use in command building
	const command = `Start-Process ${executePowerShell.escapeArgument(appName)}`;
	```
	*/
	export function escapeArgument(value: unknown): string;

	/**
	Create the full arguments array for PowerShell execution.

	Combines `argumentsPrefix` with the encoded command. Useful when using `spawn()`, `execFile()`, or other process execution methods.

	@param command - The PowerShell command.
	@returns Array of arguments ready to pass to a process spawner.

	@example
	```
	import {spawn} from 'node:child_process';
	import {powerShellPath, executePowerShell} from 'powershell-utils';

	const args = executePowerShell.createArguments('Get-Process');
	spawn(powerShellPath(), args);
	```
	*/
	export function createArguments(command: string): string[];
}

/**
Execute a PowerShell command synchronously.

@param command - The PowerShell command to execute.
@returns The stdout output as a string.

@example
```
import {executePowerShellSync} from 'powershell-utils';

const stdout = executePowerShellSync('Get-Process');
console.log(stdout);
```
*/
export function executePowerShellSync(
	command: string,
	options?: ExecutePowerShellSyncOptions
): string;
