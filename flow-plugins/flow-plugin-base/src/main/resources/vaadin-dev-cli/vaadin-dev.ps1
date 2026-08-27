<#
    vaadin-dev.ps1 - the Windows port of the vaadin-dev CLI.

    The bash script beside this file is the reference implementation; this
    reproduces its contract exactly, because the exit code is what agents depend
    on and it has to mean the same thing on all three platforms:

        0  the change is live (or there was nothing to do)
        1  failed
        4  superseded by a newer apply
        64 usage
        70 internal / the daemon cannot be reached
        77 unauthorized

    The only real difference is the transport: bash has /dev/tcp, PowerShell has
    System.Net.Sockets.TcpClient.

    Installed by `mvn vaadin:install-dev-cli` and meant to be committed, like
    mvnw. Rewritten by that goal whenever it changes, so edits here do not
    survive an upgrade.
#>

Set-StrictMode -Version Latest
# A failing cmdlet should stop the script rather than be stepped over. Native
# commands are the exception and are run through Invoke-Native below: under 'Stop'
# PowerShell turns every line a native command writes to stderr into a terminating
# error, and Maven's stderr routinely carries JVM warnings that mean nothing.
$ErrorActionPreference = 'Stop'

# --- where this script is, and which application it acts on -------------------
# Two different questions, kept apart: $scriptDir is where this file lives,
# $root is the application the command acts on.
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# The install goal puts this in the application's own .vaadin/ directory - the
# same one the daemon keeps its handshake in - which is a directory of tooling and
# state and never an application, so the application is the one above it. Anywhere
# else, the script sits in the application directory.
if ((Split-Path -Leaf $scriptDir) -eq '.vaadin') {
    $scriptApp = Split-Path -Parent $scriptDir
} else {
    $scriptApp = $scriptDir
}

function Write-Usage {
    @'
usage: vaadin-dev [--app <dir>] <command> [options]

  status [--json]   app up? owner? dev server? current/last transaction
  apply [--json]    commit pending edits; blocks until Stable or Failed
                    (--no-restart to stop after the compile gate)
  start             launch the app in dev mode (daemon owns it)
  stop              stop the app
  restart           stop then start
  shutdown          stop the daemon (and the app it owns)
  ping              check the daemon is alive

  redefine <a.b.C,...>
                    diagnostic: push named classes at the running app and print
                    the raw reply, without apply's escalation policy

The daemon starts automatically on first use and serves this application only.
See vaadin-dev (the bash script beside this one) for the full option and
environment-variable reference; both read the same variables.
'@
}

# --- argument handling --------------------------------------------------------
# --app is consumed here, before anything derives a path from $root, and stripped
# from the arguments so the daemon never sees it.
$appOverride = $env:VAADIN_DEV_APP
$rest = New-Object System.Collections.Generic.List[string]
for ($i = 0; $i -lt $args.Count; $i++) {
    $argument = [string]$args[$i]
    if ($argument -eq '--app') {
        $i++
        if ($i -ge $args.Count) {
            [Console]::Error.WriteLine('vaadin-dev: --app needs a directory')
            exit 64
        }
        $appOverride = [string]$args[$i]
    } elseif ($argument -like '--app=*') {
        $appOverride = $argument.Substring('--app='.Length)
    } else {
        $rest.Add($argument)
    }
}

if ($appOverride) {
    if (-not (Test-Path -LiteralPath $appOverride -PathType Container)) {
        [Console]::Error.WriteLine("vaadin-dev: no such directory: $appOverride")
        exit 64
    }
    $root = (Resolve-Path -LiteralPath $appOverride).ProviderPath
    if (-not (Test-Path -LiteralPath (Join-Path $root 'pom.xml'))) {
        [Console]::Error.WriteLine("vaadin-dev: not a Maven module (no pom.xml): $root")
        exit 64
    }
    # An override is the one case where the subject of the answer is not obvious
    # from where the script sits, so the answer says which application it is for.
    if ($root -ne $scriptApp) {
        [Console]::Error.WriteLine("vaadin-dev: application $root")
    }
} else {
    $root = $scriptApp
}

# The handshake stays outside target/ on purpose: `mvn clean` must not orphan a
# running daemon and leave the next command spawning a second one to fight for
# port 8080.
$handshake = Join-Path $root '.vaadin\daemon.properties'
$workDir = Join-Path $root 'target\devloop'
$daemonLog = Join-Path $workDir 'daemon.log'
$daemonJarCache = Join-Path $workDir 'daemon-jar.txt'
$pomStamp = Join-Path $workDir 'cp.stamp'

# --- the handshake file ------------------------------------------------------
function Read-Handshake([string] $key) {
    if (-not (Test-Path -LiteralPath $handshake)) { return $null }
    foreach ($line in Get-Content -LiteralPath $handshake) {
        if ($line -match "^$([regex]::Escape($key))=(.*)$") {
            $value = $Matches[1]
            if ($value) { return $value }
        }
    }
    return $null
}

function Get-JavaBinary {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return 'java.exe'
}

# --- resolving the daemon jar ------------------------------------------------
# Runs a native command and returns its exit code together with everything it
# printed, stderr included.
#
# Two things here are not decoration. First, the ErrorActionPreference dance:
# PowerShell wraps each line a native command writes to stderr in a
# NativeCommandError record, and under the 'Stop' this script sets that record is a
# terminating error - so `mvn` emitting "WARNING: A restricted method in
# java.lang.System has been called", which every recent JVM does on stderr while
# succeeding, aborted the command instead of resolving the daemon jar. Second, the
# output is captured and returned rather than redirected to a file: PowerShell
# 5.1's `>` and `>>` write UTF-16LE, which leaves a log no other tool can read.
# The exit code is the only thing that says whether the command actually failed.
function Invoke-Native([string] $file, [string[]] $arguments) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $printed = @(& $file @arguments 2>&1 | ForEach-Object { "$_" })
        return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $printed }
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Get-MavenBinary {
    $dir = $root
    while ($dir) {
        foreach ($name in @('mvnw.cmd', 'mvnw.bat')) {
            $candidate = Join-Path $dir $name
            if (Test-Path -LiteralPath $candidate) { return $candidate }
        }
        $parent = Split-Path -Parent $dir
        if ($parent -eq $dir) { break }
        $dir = $parent
    }
    $onPath = Get-Command 'mvn' -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    return $null
}

# -Dvaadin.dev.daemonJar=<path> out of VAADIN_DEV_DAEMON_OPTS. Read here rather
# than passed through to the JVM, because it selects the jar the JVM is given.
function Get-DaemonJarOption {
    if (-not $env:VAADIN_DEV_DAEMON_OPTS) { return $null }
    $match = [regex]::Match($env:VAADIN_DEV_DAEMON_OPTS,
        '-Dvaadin\.dev\.daemonJar=(.*?)(?= -D|$)')
    if ($match.Success) { return $match.Groups[1].Value }
    return $null
}

# The daemon jar named by a resolved-classpath file.
#
# Normally the file holds exactly that one path, because the resolve filters by
# artifact id. It is parsed rather than read whole anyway, so a plugin version
# that ignores the filter - or a hand-written cache holding a full classpath - is
# read correctly instead of failing with "this project does not depend on the
# daemon".
function Get-DaemonJarFrom([string] $file) {
    if (-not (Test-Path -LiteralPath $file)) { return $null }
    $content = ((Get-Content -LiteralPath $file -Raw) -replace '[\r\n]', '')
    if (-not $content) { return $null }
    if (Test-Path -LiteralPath $content) { return $content }
    foreach ($entry in ($content -split [System.IO.Path]::PathSeparator)) {
        if ($entry -match 'flow-devloop-daemon[^\\/]*\.jar$' `
                -and (Test-Path -LiteralPath $entry)) {
            return $entry
        }
    }
    return $null
}

# Answering from a one-line file is what keeps `vaadin-dev status` in the
# milliseconds.
function Get-CachedDaemonJar {
    $jar = Get-DaemonJarFrom $daemonJarCache
    if (-not $jar) { return $null }
    $cacheWritten = (Get-Item -LiteralPath $daemonJarCache).LastWriteTimeUtc
    # A pom edit anywhere in the reactor moves the stamp the daemon maintains;
    # the application's own pom covers the case where no daemon has run yet.
    $appPom = Join-Path $root 'pom.xml'
    if ((Get-Item -LiteralPath $appPom).LastWriteTimeUtc -gt $cacheWritten) { return $null }
    if (Test-Path -LiteralPath $pomStamp) {
        if ((Get-Item -LiteralPath $pomStamp).LastWriteTimeUtc -gt $cacheWritten) { return $null }
    }
    return $jar
}

function Resolve-DaemonJar {
    $maven = Get-MavenBinary
    if (-not $maven) {
        [Console]::Error.WriteLine('vaadin-dev: no Maven wrapper and no mvn on PATH, so the daemon jar cannot be resolved. Set VAADIN_DEV_HOME to a directory containing flow-devloop-daemon.jar.')
        return $null
    }
    New-Item -ItemType Directory -Force -Path $workDir | Out-Null
    $log = Join-Path $workDir 'daemon-jar-resolve.log'
    # Both attempts are kept, so a failure report shows the offline one as well as
    # the online one rather than only the last.
    $transcript = New-Object System.Collections.Generic.List[string]
    $previous = Get-Location
    Set-Location -LiteralPath $root
    try {
        # Offline first: it is the fast path, and the jar is already in the local
        # repository whenever the project has been built once.
        $ok = $false
        foreach ($mode in @('-o', '-nsu')) {
            $result = Invoke-Native $maven @('-B', '-ntp', '-q', $mode,
                'dependency:build-classpath',
                '-DincludeArtifactIds=flow-devloop-daemon',
                '-Dmdep.outputFile=target/devloop/daemon-jar.txt',
                '-Dmdep.regenerateFile=true')
            $transcript.Add("--- $maven $mode (exit $($result.ExitCode)) ---")
            if ($result.Output) { $transcript.AddRange([string[]]$result.Output) }
            if ($result.ExitCode -eq 0) { $ok = $true; break }
            Step-Spinner
        }
    } finally {
        Set-Location -LiteralPath $previous
    }
    # UTF-8, written once, so the log reads the same here as the one bash writes.
    [System.IO.File]::WriteAllLines($log, $transcript)
    if (-not $ok) {
        [Console]::Error.WriteLine("vaadin-dev: could not resolve flow-devloop-daemon; last lines of ${log}:")
        $tail = $transcript
        if ($transcript.Count -gt 15) {
            $tail = $transcript.GetRange($transcript.Count - 15, 15)
        }
        foreach ($line in $tail) { [Console]::Error.WriteLine("  $line") }
        return $null
    }
    $jar = Get-DaemonJarFrom $daemonJarCache
    if (-not $jar) {
        [Console]::Error.WriteLine("vaadin-dev: this project does not depend on the dev-loop daemon. Add com.vaadin:vaadin-dev (optional) to $root\pom.xml.")
        return $null
    }
    return $jar
}

# Overrides first, then the cache, then Maven. The daemon jar is also the
# javaagent - it carries Premain-Class alongside Main-Class - so this one path is
# all the app JVM needs.
function Get-DaemonJar {
    $configured = Get-DaemonJarOption
    if ($configured) {
        if (-not (Test-Path -LiteralPath $configured)) {
            [Console]::Error.WriteLine("vaadin-dev: -Dvaadin.dev.daemonJar does not exist: $configured")
            return $null
        }
        return $configured
    }
    if ($env:VAADIN_DEV_HOME) {
        $homeJar = Join-Path $env:VAADIN_DEV_HOME 'flow-devloop-daemon.jar'
        if (-not (Test-Path -LiteralPath $homeJar)) {
            [Console]::Error.WriteLine("vaadin-dev: no flow-devloop-daemon.jar in VAADIN_DEV_HOME=$($env:VAADIN_DEV_HOME)")
            return $null
        }
        return $homeJar
    }
    $cached = Get-CachedDaemonJar
    if ($cached) { return $cached }
    return Resolve-DaemonJar
}

# --- the progress line -------------------------------------------------------
# A start or an apply goes quiet for tens of seconds while a JVM boots or javac
# runs, and a silent terminal is indistinguishable from a wedged one. Drawn on
# stderr, and only for a human: stdout stays exactly the lines the daemon sent,
# so `status --json`, a pipe and an agent all see the same bytes as on bash.
$script:spinFrames = '|/-\'
$script:spinIndex = 0
$script:spinLabel = ''
$script:spinSince = [datetime]::UtcNow
$script:spinQuiet = [datetime]::UtcNow
$script:spinDrawn = 0

function Test-SpinnerEnabled {
    switch ($env:VAADIN_DEV_PROGRESS) {
        'never' { return $false }
        'always' { return $true }
        default { return -not [Console]::IsErrorRedirected }
    }
}

function Start-Spinner([string] $label) {
    $script:spinLabel = $label
    $script:spinSince = [datetime]::UtcNow
    $script:spinQuiet = [datetime]::UtcNow
    $script:spinIndex = 0
}

function Clear-Spinner {
    if ((Test-SpinnerEnabled) -and $script:spinDrawn -gt 0) {
        [Console]::Error.Write("`r" + (' ' * $script:spinDrawn) + "`r")
        $script:spinDrawn = 0
    }
}

function Step-Spinner {
    if (-not (Test-SpinnerEnabled)) { return }
    $frame = $script:spinFrames[$script:spinIndex % $script:spinFrames.Length]
    $script:spinIndex++
    $quiet = [int]([datetime]::UtcNow - $script:spinQuiet).TotalSeconds
    $note = ''
    if ($quiet -gt 20) { $note = "  (quiet ${quiet}s)" }
    $elapsed = [int]([datetime]::UtcNow - $script:spinSince).TotalSeconds
    # Sized to the window: a label that wraps to a second row would leave the
    # first one behind on the next repaint.
    $room = 60
    try { $room = [Math]::Max(20, [Console]::WindowWidth - 20) } catch { $room = 60 }
    $label = $script:spinLabel
    if ($label.Length -gt $room) { $label = $label.Substring(0, $room) }
    $text = "$frame $label  ${elapsed}s$note"
    [Console]::Error.Write("`r" + (' ' * $script:spinDrawn) + "`r" + $text)
    $script:spinDrawn = $text.Length
}

# --- the daemon --------------------------------------------------------------
function Start-Daemon {
    $jar = Get-DaemonJar
    if (-not $jar) { return $false }
    New-Item -ItemType Directory -Force -Path $workDir | Out-Null

    $arguments = New-Object System.Collections.Generic.List[string]
    if ($env:VAADIN_DEV_DAEMON_OPTS) {
        foreach ($option in ($env:VAADIN_DEV_DAEMON_OPTS -split '\s+')) {
            if ($option) { $arguments.Add($option) }
        }
    }
    $arguments.Add('-jar')
    $arguments.Add($jar)
    $arguments.Add($root)

    # Detached: the daemon outlives this shell so later commands reuse it.
    # Start-Process redirects straight to files, so nothing here has to pump a
    # pipe - a pipe nobody drains would wedge the daemon once its buffer filled.
    # Two files rather than one because Start-Process cannot merge the streams;
    # the daemon writes everything but an accept() failure to stdout.
    Start-Process -FilePath (Get-JavaBinary) -ArgumentList $arguments `
        -WorkingDirectory $root -WindowStyle Hidden `
        -RedirectStandardOutput $daemonLog `
        -RedirectStandardError (Join-Path $workDir 'daemon-err.log') | Out-Null

    # Wait for the handshake file rather than sleeping a fixed amount.
    Start-Spinner 'starting daemon'
    for ($waited = 0; $waited -lt 200; $waited++) {
        if ((Test-Path -LiteralPath $handshake) -and (Read-Handshake 'port')) {
            Clear-Spinner
            return $true
        }
        Start-Sleep -Milliseconds 100
        if ($waited % 2 -eq 0) { Step-Spinner }
    }
    Clear-Spinner
    # Same rule as the app: the reason exists only in the log the process wrote,
    # so it comes back with the failure instead of being left there to be found.
    [Console]::Error.WriteLine("vaadin-dev: daemon did not come up; last lines of ${daemonLog}:")
    if (Test-Path -LiteralPath $daemonLog) {
        Get-Content -LiteralPath $daemonLog -Tail 15 |
            ForEach-Object { [Console]::Error.WriteLine("  $_") }
    }
    return $false
}

# --- the wire ----------------------------------------------------------------
# Sends one command and streams the reply. Response lines are "> text" progress
# followed by a final "EXIT <code>" which becomes this script's exit status.
# 99 is this function's own: the recorded daemon is not answering.
function Send-Command([int] $port, [string] $token, [string[]] $words) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect([System.Net.IPAddress]::Loopback, $port)
    } catch {
        return 99
    }
    try {
        $stream = $client.GetStream()
        $encoding = New-Object System.Text.UTF8Encoding($false)
        $writer = New-Object System.IO.StreamWriter($stream, $encoding)
        $writer.AutoFlush = $true
        $reader = New-Object System.IO.StreamReader($stream, $encoding)
        $writer.WriteLine(($token + ' ' + ($words -join ' ')))

        Start-Spinner ($words -join ' ')
        # A read timeout is what makes room for a frame between two replies. A
        # timeout leaves the characters StreamReader has already decoded in its
        # own buffer, so the next call continues the same line rather than losing
        # its start - which is what bash's partial-read handling does by hand.
        $stream.ReadTimeout = 200
        $status = 1
        while ($true) {
            $chunk = $null
            try {
                $chunk = $reader.ReadLine()
            } catch [System.IO.IOException] {
                Step-Spinner
                continue
            }
            if ($null -eq $chunk) {
                # End of stream, which for a command that never sent EXIT means
                # the daemon went away mid-reply.
                Clear-Spinner
                break
            }
            $line = $chunk.TrimEnd("`r")
            # Erase first: a reply line must never be printed onto a drawn frame.
            Clear-Spinner
            if ($line -like 'EXIT *') {
                $status = [int]($line.Substring('EXIT '.Length).Trim())
                break
            } elseif ($line -like '> *') {
                $text = $line.Substring('> '.Length)
                [Console]::Out.WriteLine($text)
                # The last thing the daemon said is the truest label available: it
                # names the phase, so the frame moves under "restarting".
                $script:spinLabel = $text
            } else {
                [Console]::Out.WriteLine($line)
            }
            $script:spinQuiet = [datetime]::UtcNow
        }
        Clear-Spinner
        return $status
    } finally {
        $client.Close()
    }
}

# --- main --------------------------------------------------------------------
if ($rest.Count -eq 0 -or $rest[0] -in @('-h', '--help', 'help')) {
    Write-Usage
    exit 0
}
$command = $rest[0]
$commandArgs = @($rest | Select-Object -Skip 1)
$words = @($command) + $commandArgs

try {
    $port = Read-Handshake 'port'
    $token = Read-Handshake 'token'
    if (-not $port -or -not $token) {
        if (-not (Start-Daemon)) { exit 70 }
        $port = Read-Handshake 'port'
        $token = Read-Handshake 'token'
    }

    $status = Send-Command ([int]$port) $token $words

    # 99 means the recorded daemon is not answering: the record is stale, so reap
    # it and try once more with a fresh daemon.
    if ($status -eq 99) {
        Remove-Item -LiteralPath $handshake -Force -ErrorAction SilentlyContinue
        if (-not (Start-Daemon)) { exit 70 }
        $port = Read-Handshake 'port'
        $token = Read-Handshake 'token'
        $status = Send-Command ([int]$port) $token $words
        if ($status -eq 99) {
            [Console]::Error.WriteLine('vaadin-dev: cannot reach daemon')
            exit 70
        }
    }
    exit $status
} finally {
    Clear-Spinner
}
