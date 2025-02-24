if ([string]::IsNullOrEmpty($Env:INTELLIJ_TERMINAL_COMMAND_BLOCKS)) {
  return
}

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Global:__JetBrainsIntellijEncode([string]$value) {
  $Bytes = [System.Text.Encoding]::UTF8.GetBytes($value)
  return [System.BitConverter]::ToString($Bytes).Replace("-", "")
}

function Global:__JetBrainsIntellijOSC([string]$body) {
  return "$([char]0x1B)]1341;$body`a"
  # ConPTY processes custom OSC asynchronously with regular output.
  # Let's use C1 control codes for OSC to fool ConPTY and output
  # the escape sequence in proper position of the regular output.
  # return "$([char]0x9D)1341;$body$([char]0x9C)"
}

function Global:__JetBrainsIntellijGetCommandEndMarker() {
  $CommandEndMarker = $Env:JETBRAINS_INTELLIJ_COMMAND_END_MARKER
  if ($CommandEndMarker -eq $null) {
    $CommandEndMarker = ""
  }
  return $CommandEndMarker
}

$Global:__JetBrainsIntellijTerminalInitialized=$false
$Global:__JetBrainsIntellijGeneratorRunning=$false

function Global:Prompt() {
  $Success = $?
  $ExitCode = $Global:LastExitCode
  $Global:LastExitCode = 0
  if ($Global:__JetBrainsIntellijGeneratorRunning) {
    $Global:__JetBrainsIntellijGeneratorRunning = $false
    # Hide internal command in the built-in session history.
    # See "Set-PSReadLineOption -AddToHistoryHandler" for hiding same commands in the PSReadLine history.
    Clear-History -CommandLine "__JetBrainsIntellijGetCompletions*",`
                               "__jetbrains_intellij_get_environment*",`
                               "__jetbrains_intellij_get_directory_files*"
    return ""
  }

  $Result = ""
  $CommandEndMarker = Global:__JetBrainsIntellijGetCommandEndMarker
  $PromptStateOSC = Global:__JetBrainsIntellijCreatePromptStateOSC
  if ($__JetBrainsIntellijTerminalInitialized) {
    if (($ExitCode -eq $null) -or ($ExitCode -eq 0 -and -not $Success)) {
      $ExitCode = if ($Success) { 0 } else { 1 }
    }
    if ($Env:JETBRAINS_INTELLIJ_TERMINAL_DEBUG_LOG_LEVEL) {
      [Console]::WriteLine("command_finished exit_code=$ExitCode")
    }
    $CommandFinishedEvent = Global:__JetBrainsIntellijOSC "command_finished;exit_code=$ExitCode"
    $Result = $CommandEndMarker + $PromptStateOSC + $CommandFinishedEvent
  }
  else {
    # For some reason there is no error if I delete the history file, just an empty string returned.
    # There can be a check for file existence using Test-Path cmdlet, but if I add it, the prompt is failed to initialize.
    $History = Get-Content -Raw (Get-PSReadlineOption).HistorySavePath
    $HistoryOSC = Global:__JetBrainsIntellijOSC "command_history;history_string=$(__JetBrainsIntellijEncode $History)"

    $Global:__JetBrainsIntellijTerminalInitialized = $true
    if ($Env:JETBRAINS_INTELLIJ_TERMINAL_DEBUG_LOG_LEVEL) {
      [Console]::WriteLine("initialized")
    }
    $InitializedEvent = Global:__JetBrainsIntellijOSC "initialized"
    $Result = $CommandEndMarker + $PromptStateOSC + $HistoryOSC + $InitializedEvent
  }
  return $Result
}

function Global:__JetBrainsIntellijCreatePromptStateOSC() {
  # Remember the exit code, because it can be changed in a result of git operations
  $RealExitCode = $Global:LastExitCode

  $CurrentDirectory = (Get-Location).Path
  $GitBranch = ""
  if (Get-Command "git.exe" -ErrorAction SilentlyContinue) {
    $GitBranch = git.exe symbolic-ref --short HEAD 2>$null
    if ($GitBranch -eq $null) {
      # get the current revision hash, if not on the branch
      $GitBranch = git.exe rev-parse --short HEAD 2>$null
      if ($GitBranch -eq $null) {
        $GitBranch = ""
      }
    }
  }
  $VirtualEnv = if ($Env:VIRTUAL_ENV -ne $null) { $Env:VIRTUAL_ENV } else { "" }
  $CondaEnv = if ($Env:CONDA_DEFAULT_ENV -ne $null) { $Env:CONDA_DEFAULT_ENV } else { "" }
  $StateOSC = Global:__JetBrainsIntellijOSC ("prompt_state_updated;" +
    "current_directory=$(__JetBrainsIntellijEncode $CurrentDirectory);" +
    "git_branch=$(__JetBrainsIntellijEncode $GitBranch);" +
    "virtual_env=$(__JetBrainsIntellijEncode $VirtualEnv);" +
    "conda_env=$(__JetBrainsIntellijEncode $CondaEnv)")

  $Global:LastExitCode = $RealExitCode
  return $StateOSC
}

function Global:__JetBrainsIntellij_ClearAllAndMoveCursorToTopLeft() {
  [Console]::Clear()
}

function Global:__JetBrainsIntellijGetCompletions([int]$RequestId, [string]$Command, [int]$CursorIndex) {
  $Global:__JetBrainsIntellijGeneratorRunning = $true
  $Completions = TabExpansion2 -inputScript $Command -cursorColumn $CursorIndex
  if ($null -ne $Completions) {
    $CompletionsJson = $Completions | ConvertTo-Json -Compress
  }
  else {
    $CompletionsJson = ""
  }
  $CompletionsOSC = Global:__JetBrainsIntellijOSC "generator_finished;request_id=$RequestId;result=$(__JetBrainsIntellijEncode $CompletionsJson)"
  $CommandEndMarker = Global:__JetBrainsIntellijGetCommandEndMarker
  [Console]::Write($CommandEndMarker + $CompletionsOSC)
}

function Global:__jetbrains_intellij_get_directory_files([int]$RequestId, [string]$Path) {
  $Global:__JetBrainsIntellijGeneratorRunning = $true
  $Files = Get-ChildItem -Force -Path $Path | Where { $_ -is [System.IO.FileSystemInfo] }
  $Separator = [System.IO.Path]::DirectorySeparatorChar
  $FileNames = $Files | ForEach-Object { if ($_ -is [System.IO.DirectoryInfo]) { $_.Name + $Separator } else { $_.Name } }
  $FilesString = $FileNames -join "`n"
  $FilesOSC = Global:__JetBrainsIntellijOSC "generator_finished;request_id=$RequestId;result=$(__JetBrainsIntellijEncode $FilesString)"
  $CommandEndMarker = Global:__JetBrainsIntellijGetCommandEndMarker
  [Console]::Write($CommandEndMarker + $FilesOSC)
}

function Global:__jetbrains_intellij_get_environment([int]$RequestId) {
  $Global:__JetBrainsIntellijGeneratorRunning = $true
  $Functions = Get-Command -CommandType "Function, Filter, ExternalScript, Script, Workflow"
  $Cmdlets = Get-Command -CommandType Cmdlet
  $Commands = Get-Command -CommandType Application
  $Aliases = Get-Alias | ForEach-Object { [PSCustomObject]@{ name = $_.Name; definition = $_.Definition } }

  $EnvObject = [PSCustomObject]@{
    envs = ""
    keywords = ""
    builtins = ($Cmdlets | ForEach-Object { $_.Name }) -join "`n"
    functions = ($Functions | ForEach-Object { $_.Name }) -join "`n"
    commands = ($Commands | ForEach-Object { $_.Name }) -join "`n"
    aliases = $Aliases | ConvertTo-Json -Compress
  }
  $EnvJson = $EnvObject | ConvertTo-Json -Compress
  $EnvOSC = Global:__JetBrainsIntellijOSC "generator_finished;request_id=$RequestId;result=$(__JetBrainsIntellijEncode $EnvJson)"
  $CommandEndMarker = Global:__JetBrainsIntellijGetCommandEndMarker
  [Console]::Write($CommandEndMarker + $EnvOSC)
}

function Global:__JetBrainsIntellijIsGeneratorCommand([string]$Command) {
  return $Command -like "__JetBrainsIntellijGetCompletions*" `
         -or $Command -like "__jetbrains_intellij_get_environment*" `
         -or $Command -like "__jetbrains_intellij_get_directory_files*"
}

# Override the clear cmdlet to handle it on IDE side and remove the blocks
function Global:Clear-Host() {
  $OSC = Global:__JetBrainsIntellijOSC "clear_invoked"
  [Console]::Write($OSC)
}

if (Get-Module -Name PSReadLine) {
  $Global:__JetBrainsIntellijOriginalPSConsoleHostReadLine = $function:PSConsoleHostReadLine

  function Global:PSConsoleHostReadLine {
    $OriginalReadLine = $Global:__JetBrainsIntellijOriginalPSConsoleHostReadLine.Invoke()
    if (__JetBrainsIntellijIsGeneratorCommand $OriginalReadLine) {
      return $OriginalReadLine
    }

    $CurrentDirectory = (Get-Location).Path
    if ($Env:JETBRAINS_INTELLIJ_TERMINAL_DEBUG_LOG_LEVEL) {
      [Console]::WriteLine("command_started $OriginalReadLine")
    }
    $CommandStartedOSC = Global:__JetBrainsIntellijOSC "command_started;command=$(__JetBrainsIntellijEncode $OriginalReadLine);current_directory=$(__JetBrainsIntellijEncode $CurrentDirectory)"
    [Console]::Write($CommandStartedOSC)
    Global:__JetBrainsIntellij_ClearAllAndMoveCursorToTopLeft
    return $OriginalReadLine
  }

  $Global:__JetBrainsIntellijOriginalAddToHistoryHandler = (Get-PSReadLineOption).AddToHistoryHandler

  Set-PSReadLineOption -AddToHistoryHandler {
    param([string]$Command)
    if (__JetBrainsIntellijIsGeneratorCommand $Command) {
      return $false
    }
    if ($Global:__JetBrainsIntellijOriginalAddToHistoryHandler -ne $null) {
      return $Global:__JetBrainsIntellijOriginalAddToHistoryHandler.Invoke($Command)
    }
    return $true
  }
}
