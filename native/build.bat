@echo off
setlocal enabledelayedexpansion
cd /d %~dp0

rem 32ビット版ProgramFilesの位置
if "%ProgramFiles(x86)%" == "" set ProgramFiles(x86)=%ProgramFiles%

rem 実プロセスアーキテクチャの検出
set ARCH=%PROCESSOR_ARCHITEW6432%
if "%ARCH%" == "" set ARCH=%PROCESSOR_ARCHITECTURE%

rem 32ビット版レジストリ位置
if "%ARCH%" == "AMD64" (
set REGPREFIX=HKEY_LOCAL_MACHINE\Software\Wow6432Node
) else (
set REGPREFIX=HKEY_LOCAL_MACHINE\Software
)

rem Visual Studioの環境変数設定バッチファイルの位置
echo Searching...

rem Visual Studio 2010
if exist "%ProgramFiles(x86)%\Microsoft Visual Studio 10.0\VC\vcvarsall.bat" (
set VS_DIR="%ProgramFiles(x86)%\Microsoft Visual Studio 10.0\"
set VCVARS="%ProgramFiles(x86)%\Microsoft Visual Studio 10.0\VC\vcvarsall.bat"
set VSVER=10.0
set PLATFORM_TOOLSET=v100
echo found !VS_DIR!
)

rem Visual Studio 2012
if exist "%ProgramFiles(x86)%\Microsoft Visual Studio 11.0\VC\vcvarsall.bat" (
set VS_DIR="%ProgramFiles(x86)%\Microsoft Visual Studio 11.0\"
set VCVARS="%ProgramFiles(x86)%\Microsoft Visual Studio 11.0\VC\vcvarsall.bat"
set VSVER=11.0
set PLATFORM_TOOLSET=v110
echo found !VS_DIR!
)

rem Visual Studio 2013
for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\VisualStudio\SxS\VS7" /v "12.0" 2^> nul') do if "%%A" == "12.0" (
set VS_DIR="%%C"
set VCVARS="%%CVC\vcvarsall.bat"
set VSVER=12.0
set PLATFORM_TOOLSET=v120
echo found !VS_DIR!
)

rem Visual Studio 2015
for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\VisualStudio\SxS\VS7" /v "14.0" 2^> nul') do if "%%A" == "14.0" (
set VS_DIR="%%C"
set VCVARS="%%CVC\vcvarsall.bat"
set VSVER=14.0
set PLATFORM_TOOLSET=v140
echo found !VS_DIR!
)

rem Visual Studio 2017
for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\VisualStudio\SxS\VS7" /v "15.0" 2^> nul') do if "%%A" == "15.0" (
set VS_DIR="%%C"
set VCVARS="%%CVC\Auxiliary\Build\vcvarsall.bat"
set VSVER=15.0
set PLATFORM_TOOLSET=v141
echo found !VS_DIR!
)

rem WindowsSDKの検索 (Win10SDKのみ検索)

for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\Microsoft SDKs\Windows\v10.0" /v "ProductVersion" 2^> nul') do if "%%A" == "ProductVersion" (
set WINSDK_VERSION=%%C.0
echo found ! Win10 SDK %%C
)

echo.
echo Determined:
echo VSVER=%VSVER%
echo VSDIR=%VS_DIR%
echo VCVARS=%VCVARS%
echo.
echo ARCH=%ARCH%
echo WINSDK_VERSION=%WINSDK_VERSION%
echo PLATFORM_TOOLSET=%PLATFORM_TOOLSET%
echo.

if '%VCVARS%' == '' (
	echo ERROR: vcvarsall.bat not found >&2
	exit /b 2
)

rem VC++のx86ビルド用の環境変数の設定
rem (VS2017では、VSCMD_START_DIRが指定ない場合は、カレントが移動するため、現在位置を指定しておく)
set VSCMD_START_DIR=%CD%
call %VCVARS% %ARCH%

rem VSディレクトリの表示
echo VSINSTALLDIR=%VSINSTALLDIR%

rem 検出したSDKとVSにあわせて、MSBuildのPlatformToolsetと、WindowsTargetPlatformVersionのプロパティをオーバーライドする
if not "%PLATFORM_TOOLSET%" == "" set OPT_PLATFORM_TOOLSET=/p:PlatformToolset=%PLATFORM_TOOLSET%
if not "%WINSDK_VERSION%" == "" set OPT_WINSDK_VERSION=/p:WindowsTargetPlatformVersion=%WINSDK_VERSION%

rem MSBUILD呼び出し
 msbuild /m /t:rebuild /p:Configuration=Release /p:Platform="win32" %OPT_PLATFORM_TOOLSET% %OPT_WINSDK_VERSION%
if errorlevel 1 goto err

if "%ARCH%" == "AMD64" (
  msbuild /m /t:rebuild /p:Configuration=Release /p:Platform="x64" %OPT_PLATFORM_TOOLSET% %OPT_WINSDK_VERSION%
  if errorlevel 1 goto err
)

if "%1" == "" pause
exit /b 0

:ERR
if "%1" == "" pause
exit /b 1


