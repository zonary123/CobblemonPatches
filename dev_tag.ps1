# Obtener versión de gradle.properties
$gradleFile = "gradle.properties"
if (-Not (Test-Path $gradleFile)) {
    Write-Error "No se encontró gradle.properties"
    exit 1
}

$versionLine = Get-Content $gradleFile | Where-Object { $_ -match '^mod_version=' }
if (-Not $versionLine) {
    Write-Error "No se encontró mod_version en gradle.properties"
    exit 1
}

$version = ($versionLine -split '=')[1].Trim()

# Obtener hash corto de git
try {
    $gitHash = git rev-parse --short HEAD
} catch {
    Write-Error "No se pudo obtener el hash de git. ¿Estás en un repositorio git?"
    exit 1
}

# Limpiar versión de caracteres inválidos para tags
$safeVersion = -join ($version.ToCharArray() | Where-Object { $_ -match '[a-zA-Z0-9._-]' })

# Construir tag de prueba
$tag = "v${safeVersion}-dev-${gitHash}"

# Crear tag localmente (si ya existe, avisar)

    git tag -a $tag -m "Dev build $tag"
    git push origin $tag
    Write-Host "Tag creado y subido: $tag"

