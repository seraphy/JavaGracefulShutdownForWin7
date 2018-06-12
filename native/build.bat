@echo off
setlocal enabledelayedexpansion
cd /d %~dp0

rem 32ビット版ProgramFilesの位置
if "%ProgramFiles(x86)%" == "" set ProgramFiles(x86)=%ProgramFiles%

rem 32ビット版レジストリ位置
if "%PROCESSOR_ARCHITECTURE%" == "AMD64" (
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
echo found !VS_DIR!
)

rem Visual Studio 2012
if exist "%ProgramFiles(x86)%\Microsoft Visual Studio 11.0\VC\vcvarsall.bat" (
set VS_DIR="%ProgramFiles(x86)%\Microsoft Visual Studio 11.0\"
set VCVARS="%ProgramFiles(x86)%\Microsoft Visual Studio 11.0\VC\vcvarsall.bat"
set VSVER=11.0
echo found !VS_DIR!
)

rem Visual Studio 2013
for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\VisualStudio\SxS\VS7" /v "12.0" 2^> nul') do if "%%A" == "12.0" (
set VS_DIR="%%C"
set VCVARS="%%C%VC\vcvarsall.bat"
set VSVER=12.0
echo found !VS_DIR!
)

rem Visual Studio 2015
for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\VisualStudio\SxS\VS7" /v "14.0" 2^> nul') do if "%%A" == "14.0" (
set VS_DIR="%%C"
set VCVARS="%%C%VC\vcvarsall.bat"
set VSVER=14.0
echo found !VS_DIR!
)

rem Visual Studio 2017
for /F "TOKENS=1,2,*" %%A IN ('REG QUERY "%REGPREFIX%\Microsoft\VisualStudio\SxS\VS7" /v "15.0" 2^> nul') do if "%%A" == "15.0" (
set VS_DIR="%%C"
set VCVARS="%%C%VC\Auxiliary\Build\vcvarsall.bat"
set VSVER=15.0
echo found !VS_DIR!
)

echo Determined:
echo VSVER=%VSVER%
echo VSDIR=%VS_DIR%
echo VCVARS=%VCVARS%

if '%VCVARS%' == '' (
	echo ERROR: vcvarsall.bat not found >&2
	exit /b 2
)

rem VC++のx86ビルド用の環境変数の設定
call %VCVARS% x86

rem VSディレクトリの表示
echo VSINSTALLDIR=%VSINSTALLDIR%

rem MSBUILD呼び出し
msbuild /m /t:rebuild /p:Configuration=Release /p:Platform="win32"
msbuild /m /t:rebuild /p:Configuration=Release /p:Platform="x64"

if "%1" == "" pause

