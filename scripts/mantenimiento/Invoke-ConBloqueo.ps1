function Invoke-TareaConBloqueo {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory=$true)][string]$RutaBloqueo,
        [Parameter(Mandatory=$true)][scriptblock]$Accion
    )
    $taskLockPath = [IO.Path]::GetFullPath($RutaBloqueo)
    if ([IO.Path]::GetExtension($taskLockPath) -ne '.lock') { throw 'Usar un archivo .lock dedicado.' }
    if (-not [IO.Directory]::Exists([IO.Path]::GetDirectoryName($taskLockPath))) { throw 'La carpeta del bloqueo debe existir.' }
    $taskLockHandle = $null
    try {
        $taskLockHandle = [IO.FileStream]::new($taskLockPath,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    } catch {
        $taskLockError = $_.Exception
        while ($null -ne $taskLockError.InnerException) { $taskLockError = $taskLockError.InnerException }
        $taskNativeCode = $taskLockError.HResult -band 65535
        if ($taskLockError -is [IO.IOException] -and $taskNativeCode -in @(32,33)) {
            return [pscustomobject]@{
                Estado='OmitidaPorBloqueo'; Adquirido=$false; RutaBloqueo=$taskLockPath
                CodigoNativo=$taskNativeCode; Resultado=$null
            }
        }
        # Permisos, rutas u otros errores de E/S no se ocultan como concurrencia.
        throw
    }
    try {
        $taskActionResult = & $Accion
        return [pscustomobject]@{
            Estado='Exitoso'; Adquirido=$true; RutaBloqueo=$taskLockPath
            CodigoNativo=0; Resultado=$taskActionResult
        }
    } finally {
        if ($null -ne $taskLockHandle) { $taskLockHandle.Dispose() }
        # El archivo puede permanecer: el bloqueo depende del handle, no de su existencia.
    }
}
