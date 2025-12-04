#!/bin/bash

# Leer versión de gradle.properties
VERSION=$(grep '^mod_version=' gradle.properties | cut -d'=' -f2)

# Obtener hash corto de git
GIT_HASH=$(git rev-parse --short HEAD)

# Limpiar versión por si tiene caracteres inválidos
SAFE_VERSION=$(echo "$VERSION" | tr -cd '[:alnum:]._-')

# Crear tag de prueba con versión + hash
TAG="v${SAFE_VERSION}-dev-${GIT_HASH}"

# Crear tag localmente
git tag -a "$TAG" -m "Dev build $TAG"

# Push del tag
git push origin "$TAG"

echo "Tag creado y subido: $TAG"
