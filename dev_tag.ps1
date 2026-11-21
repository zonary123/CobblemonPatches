$ErrorActionPreference = "Stop"

# ---- Comprobar que existe gradle.properties ----
$gradleFile = "gradle.properties"
if (-not (Test-Path $gradleFile))
{
    Write-Error "No se encontró gradle.properties"
    exit 1
}

# ---- Leer versión ----
$versionLine = Get-Content $gradleFile | Where-Object { $_ -match '^mod_version=' }
if (-not $versionLine)
{
    Write-Error "No se encontró mod_version en gradle.properties"
    exit 1
}

$version = ($versionLine -split '=', 2)[1].Trim()

# ---- Obtener hash git ----
$gitHash = git rev-parse --short HEAD 2> $null
if (-not $gitHash)
{
    Write-Error "No se pudo obtener el hash de git. ¿Es un repositorio git?"
    exit 1
}

# ---- Limpiar versión ----
$safeVersion = -join ($version.ToCharArray() | Where-Object { $_ -match '[a-zA-Z0-9._-]' })

# ---- Construir tag ----
$tag = "v${safeVersion}-dev-${gitHash}"

# ---- Verificar si el tag ya existe ----
$existingTag = git tag --list $tag
if ($existingTag)
{
    Write-Warning "El tag '$tag' ya existe"
    exit 1
}

# ---- Verificar que origin existe ----
$origin = git remote get-url origin 2> $null
if (-not $origin)
{
    Write-Error "El remote 'origin' no está configurado"
    exit 1
}

# ---- Crear y subir tag ----
git tag -a $tag -m "Dev build $tag"
git push origin "refs/tags/$tag"

Write-Host "Tag creado y subido correctamente: $tag"
