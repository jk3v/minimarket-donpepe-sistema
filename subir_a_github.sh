#!/usr/bin/env bash
echo "========================================================================="
echo " SINCRONIZACION DEL REPOSITORIO CON GITHUB - MINIMARKET DON PEPE"
echo " Perfil de Autor: dev-donpepe (Anonimizado para Repositorio Publico)"
echo "========================================================================="
read -p "Ingresa la URL de tu repositorio GitHub: " REPO_URL
git branch -M main
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"
git push -u origin main --tags
echo "¡Proyecto subido exitosamente a GitHub sin datos personales!"
