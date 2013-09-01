if "%VCVARSET%" == "" call "C:\Program Files (x86)\Microsoft Visual Studio 11.0\VC\vcvarsall.bat"
SET VCVARSET=Y

SET JAVA_HOME=

rem msbuild Win7Support.sln /m:4 /p:Configuration=Debug /p:Platform="win32"
rem msbuild Win7Support.sln /m:4 /p:Configuration=Debug /p:Platform="x64"
msbuild Win7Support.sln /m:4 /p:Configuration=Release /p:Platform="win32"
msbuild Win7Support.sln /m:4 /p:Configuration=Release /p:Platform="x64"
