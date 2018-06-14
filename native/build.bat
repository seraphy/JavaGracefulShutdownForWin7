@echo off
setlocal enabledelayedexpansion
cd /d %~dp0

rem 32�r�b�g��ProgramFiles�̈ʒu
if "%ProgramFiles(x86)%" == "" set ProgramFiles(x86)=%ProgramFiles%

rem ���v���Z�X�A�[�L�e�N�`���̌��o
set ARCH=%PROCESSOR_ARCHITEW6432%
if "%ARCH%" == "" set ARCH=%PROCESSOR_ARCHITECTURE%

rem 32�r�b�g�Ń��W�X�g���ʒu
if "%ARCH%" == "AMD64" (
set REGPREFIX=HKEY_LOCAL_MACHINE\Software\Wow6432Node
) else (
set REGPREFIX=HKEY_LOCAL_MACHINE\Software
)

rem Visual Studio�̊��ϐ��ݒ�o�b�`�t�@�C���̈ʒu
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

rem WindowsSDK�̌��� (Win10SDK�̂݌���)

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

rem VC++��x86�r���h�p�̊��ϐ��̐ݒ�
rem (VS2017�ł́AVSCMD_START_DIR���w��Ȃ��ꍇ�́A�J�����g���ړ����邽�߁A���݈ʒu���w�肵�Ă���)
set VSCMD_START_DIR=%CD%
call %VCVARS% %ARCH%

rem VS�f�B���N�g���̕\��
echo VSINSTALLDIR=%VSINSTALLDIR%

rem ���o����SDK��VS�ɂ��킹�āAMSBuild��PlatformToolset�ƁAWindowsTargetPlatformVersion�̃v���p�e�B���I�[�o�[���C�h����
if not "%PLATFORM_TOOLSET%" == "" set OPT_PLATFORM_TOOLSET=/p:PlatformToolset=%PLATFORM_TOOLSET%
if not "%WINSDK_VERSION%" == "" set OPT_WINSDK_VERSION=/p:WindowsTargetPlatformVersion=%WINSDK_VERSION%

rem MSBUILD�Ăяo��
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


