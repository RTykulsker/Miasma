# make sure you adjust this to point to the folder you want to monitor

$mbox="C:\Users\Bob\Desktop\miasma\output\pat\mailbox"
$PathToMonitor = "C:\Users\Bob\Desktop\miasma\output\pat\mailbox\KM6SO\out"

$pat="C:\Users\Bob\Desktop\miasma\bin\pat.exe"

### explorer $PathToMonitor

$FileSystemWatcher = New-Object System.IO.FileSystemWatcher
$FileSystemWatcher.Path  = $PathToMonitor
$FileSystemWatcher.IncludeSubdirectories = $false

# make sure the watcher emits events
$FileSystemWatcher.EnableRaisingEvents = $true

# define the code that should execute when a file change is detected
$Action = {
    $details = $event.SourceEventArgs
    $Name = $details.Name
    $FullPath = $details.FullPath
    $OldFullPath = $details.OldFullPath
    $OldName = $details.OldName
    $ChangeType = $details.ChangeType
    $Timestamp = $event.TimeGenerated
    $text = "{0} was {1} at {2}" -f $Name, $ChangeType, $Timestamp
    Write-Host ""
    Write-Host $text -ForegroundColor Green
    
    # you can also execute code based on change type here
    switch ($ChangeType)
    {
        'Changed' { "CHANGE" }
        'Created' { "CREATED"
            <#
             $vara_pid = $(get-process "varafm")
             if ($vara_pid -ne "") {
                 write-host "killing vara ..."
                 Get-Process"varafm" | Stop-Process
             }
             Write-Host "Starting varafm ..."
             Start-Process "C:\VARA FM\VARAFM.exe"

             Write-Host "Sleep for 5 seconds"
             Start-Sleep -Seconds 5

             $pat_pid = $(get_process "pat")
             if ($pat_pid -ne "") {
                 write-host "killing pat ..."
                 Get-Process"pat" | Stop-Process
             }
             #>
            Write-Host "starting pat ..."
            start-process "C:\Users\Bob\Desktop\miasma\bin\pat.exe" -ArgumentList '--mbox C:\Users\Bob\Desktop\miasma\output\pat\mailbox --ignore-busy --send-only connect telnet'
            <#
            get-process "pat"|stop-process
            Get-Process "varafm"|Stop-Process
            #>

        }
        'Deleted' { 
            $text = "File {0} was deleted" -f $Name
            Write-Host $text -ForegroundColor Red
        }
        'Renamed' { 
            # this executes only when a file was renamed
            $text = "File {0} was renamed to {1}" -f $OldName, $Name
            Write-Host $text -ForegroundColor Yellow
        }
        default { Write-Host $_ -ForegroundColor Red -BackgroundColor White }
    }
}

# add event handlers
$handlers = . {
    Register-ObjectEvent -InputObject $FileSystemWatcher -EventName Changed -Action $Action -SourceIdentifier FSChange
    Register-ObjectEvent -InputObject $FileSystemWatcher -EventName Created -Action $Action -SourceIdentifier FSCreate
    Register-ObjectEvent -InputObject $FileSystemWatcher -EventName Deleted -Action $Action -SourceIdentifier FSDelete
    Register-ObjectEvent -InputObject $FileSystemWatcher -EventName Renamed -Action $Action -SourceIdentifier FSRename
}

Write-Host "Watching for changes to $PathToMonitor"

try
{
    $dotCount = 0
    do
    {
        Wait-Event -Timeout 1
        Write-Host "." -NoNewline
        $dotCount = $dotCount + 1
        if ($dotCount -eq 80) {
            Write-Host ""
            $dotCount = 0
        }
        
    } while ($true)
}
finally
{
    # this gets executed when user presses CTRL+C
    # remove the event handlers
    Unregister-Event -SourceIdentifier FSChange
    Unregister-Event -SourceIdentifier FSCreate
    Unregister-Event -SourceIdentifier FSDelete
    Unregister-Event -SourceIdentifier FSRename
    # remove background jobs
    $handlers | Remove-Job
    # remove filesystemwatcher
    $FileSystemWatcher.EnableRaisingEvents = $false
    $FileSystemWatcher.Dispose()
    "Event Handler disabled."
}
