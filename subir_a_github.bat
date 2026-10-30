@echo off
echo =========================================================================
echo  SINCRONIZACION DEL REPOSITORIO CON GITHUB - MINIMARKET DON PEPE
echo  Perfil de Autor: dev-donpepe (Anonimizado para Repositorio Publico)
echo =========================================================================
echo 1. Crea un repositorio vacio en tu cuenta de GitHub (ej. minimarket-donpepe-system)
echo 2. Pega la URL HTTPS o SSH de tu repositorio cuando se solicite:
set /p REPO_URL="Ingresa la URL de tu repositorio GitHub: "
git branch -M main
git remote remove origin 2>nul
git remote add origin %REPO_URL%
git push -u origin main --tags
echo =========================================================================
echo  PROYECTO SUBIDO EXITOSAMENTE A GITHUB SIN DATOS PERSONALES!
echo =========================================================================
pause
