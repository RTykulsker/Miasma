$MIASMA = "$HOME\Desktop\miasma"
$MY_CALL = 'KM6SO'
$MY_EMAIL = 'RTykulsker@gmail.com'
$BIN = "$MIASMA\bin"
$PAT = "$BIN\pat.exe"

$PAT_CONNECT = "telnet"

### $PAT_MBOX = "$HOME\AppData\Local\pat\mailbox"
$PAT_MBOX="$MIASMA\output\pat\mailbox"
$PAT_PARAMS = @("--send-only", "--mbox", "$pat_mbox", "connect", "$pat_connect")

$OUTBOX = "$PAT_MBOX\$MY_CALL\out"
$SENTBOX = "$PAT_MBOX\$MY_CALL\sent"
$LOGFILE = "$MIASMA\output\pat\logfile.txt"

Set-Alias pat Run-Pat
Set-Alias go Run-Pat
Set-Alias patif Run-PatIf
Set-Alias ph Show-Help

Set-Alias log Log-Text
Set-Alias clog Clean-Log
Set-Alias logs Show-Log

Set-Alias clean Clean-Outbox
Set-Alias count Show-Count
Set-Alias show Show-Files
Set-Alias last Show-Last

Set-Alias new Compose-Message

Set-Alias mkdirs Make-Mailbox
Set-Alias rmdirs Remove-Mailbox

Set-Alias tncs Start-Tncs

function Run-Pat {
    $out_count = Get-Count
    $old_sent_count = (Get-ChildItem -Path $SENTBOX -File).Count
    log "running pat against $count files in $outbox"
    & $pat $pat_params
    $new_sent_count = (Get-ChildItem -Path $SENTBOX -File).Count
    log "sent message count was $old_sent_count now $new_sent_count"
}

function Run-PatIf {
    $out_count = Get-Count
    if ($out_count -eq 0) {
        log "skipping pat because no messages in $OUTBOX"
        return
    }
    $old_sent_count = (Get-ChildItem -Path $SENTBOX -File).Count
    log "running pat against $count files in $outbox"
    & $pat $pat_params
    $new_sent_count = (Get-ChildItem -Path $SENTBOX -File).Count
    log "sent message count was $old_sent_count now $new_sent_count"
}

function Log-Text {
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $text = $ts + ": " + $args
    Write-Output $text
    Add-Content -Path $logfile -Value $text
}

function Get-Count {
    (Get-ChildItem -Path $OUTBOX -File).Count
}

#######################################################################
# for development only
#######################################################################

function Show-Help {
    Write-Output "run-pat (pat, go): run pat against $outbox"
    Write-Output "run-patif (patif): run pat against $outbox if and only if files exist"
    Write-Output "clean-outbox (clean): remove outbox contents"
    Write-Output "compose-message (new): compose a new message"
    Write-Output "log-text (log): log text to logfile: $logfile"

    Write-output "show-count (count): show count of files in $outbox"
    write-output "show-files (show): show files in $outbox"
    write-output "show-last (last): show contents of last file in $outbox"
    write-output "show-log (logs): show contents of the $logfile"
    write-output "clean-log (clog): clear log $logfile"

    Write-output "make-mailbox (mkdirs): make maibox directory structure for $pat_mbox\$MY_call"
    Write-output "remove-mailbox (rmdirs): remove maibox directory structure for $pat_mbox\$MY_call"

    write-output "start-tncs (tncs): start Vara-FM and soundmodem TNCs"
    write-output "stop-tncs: stop Vara-FM and soundmodem TNCs"
}

function Show-Count {
    Write-Output (Get-Count)
}

function Show-Files {
    dir $OUTBOX
}

function Show-Log {
    Get-Content -Path $logfile
}

function Clean-Outbox {
    Log-Message "cleaning $OUTBOX of "(Get-Count)" file(s)"
    Get-ChildItem -Path $OUTBOX  -File | foreach { $_.Delete()}
}

function Clean-Log {
    Clear-Content -Path $LOGFILE
    log "clearing log"
}

function Generate-MessageId {
-join ('ABCDEFGHKLMNPRSTUVWXYZ23456789'.ToCharArray() | Get-Random -Count 12) 
}

function Compose-Message {
    $mid = Generate-MessageId
    $from = $MY_CALL
 
    $default = $MY_EMAIL
    $to = Read-Host -Prompt "Enter address [$default]"
    if ($to.Length -eq 0) {
        $to = $default
    }

    $default = "(no subject)"
    $subject = Read-Host -Prompt "Enter subject [$default]"
    if ($subject.Length -eq 0) {
        $subject = $default
    }

    $default = "(no body)"
    $body = Read-Host -Prompt "Enter body [$default]"
    if ($body.Length -eq 0) {
        $body = $default
    }

    $bodyLength = $body.Length
    $date = (Get-Date).ToUniversalTime().toString("yyyy/MM/dd HH:mm")
    
    $b2f = $outbox + "\" + "$mid.b2f"
    New-Item -Path "$b2f" -ItemType file 
  
    Add-Content -Path $b2f -Value "Mid: $mid"
    Add-Content -Path $b2f -Value "Body: $bodyLength"
    Add-Content -Path $b2f -Value "Content-Transfer-Encoding: 8bit"
    Add-Content -Path $b2f -Value "Date: $date"
    Add-Content -Path $b2f -Value "From: $MY_CALL"
    Add-Content -Path $b2f -Value "Mbo: $MY_CALL"
    Add-Content -Path $b2f -Value "Subject: $subject"
    Add-Content -Path $b2f -Value "To: $to"
    Add-Content -Path $b2f -Value "Type: Private"
    Add-Content -Path $b2f -Value ""
    Add-Content -Path $b2f -Value "$body"

    log-message "created message $b2f"
}

function Show-Last {
    $last = Get-ChildItem -Path $OUTBOX -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    Write-Output "Showing content of latest file in $outbox" + "\$last"
    Get-Content -Path "$outbox\$last"
}

function Show-Log {
    Get-Content $logfile
}

function Make-Mailbox {
    if (-not (Test-Path -Path "$pat_mbox")) {
        New-Item -ItemType Directory -Path $pat_mbox
        New-Item -ItemType Directory -Path "$pat_mbox\$MY_CALL"
        New-Item -ItemType Directory -Path "$pat_mbox\$MY_CALL\in"
        New-Item -ItemType Directory -Path "$pat_mbox\$MY_CALL\out"
        New-Item -ItemType Directory -Path "$pat_mbox\$MY_CALL\sent"
        New-Item -ItemType Directory -Path "$pat_mbox\$MY_CALL\archive"
        Log-Message "making folder $pat_mbox"
    }
}


function Remove-Mailbox {
    Remove-Item -Path "$pat_mbox" -Recurse
    Log-Message "removing folder $pat_mbox"
}

function Start-Tncs {
    if (1) {
        log "starting vara-fm"
        $vara_pid = $(get-process "varafm")
        if ($vara_pid -ne "") {
            log "killing vara-fm pid $vara_pid ..."
            Get-Process "varafm" | Stop-Process
        }
        log "Starting varafm ..."
        Start-Process "C:\VARA FM\VARAFM.exe"
    }

    if (1) {
        log "starting soundmodem"
        $soundmodem_pid = $(get-process "soundmodem")
        if ($soundmodem_pid -ne "") {
            log "killing soundmodem pid $soundmodem_pid ..."
            Get-Process "soundmodem" | Stop-Process
        }
        log "Starting soundmodem ..."
        Start-Process "C:\soundmodem-1.1.4\soundmodem.exe"
    }
}

function Stop-Tncs {
    $vara_pid = $(get-process "varafm")
    if ($vara_pid -ne "") {
        log "killing vara-fm pid $vara_pid ..."
        Get-Process "varafm" | Stop-Process
    }

    $soundmodem_pid = $(get-process "soundmodem")
    if ($soundmodem_pid -ne "") {
        log "killing soundmodem pid $soundmodem_pid ..."
        Get-Process "soundmodem" | Stop-Process
    }
}

#######################################################################
### this is the command, if any that gets executed at startup
#######################################################################
Run-PatIf