cd /d "%~dp0"
java -version
java -Xrs -Ddisable_win7_support=false -DshutdownWaitSecs=3 -cp bin jp.seraphyware.win7frametest.JavaForWin7Frame
pause
