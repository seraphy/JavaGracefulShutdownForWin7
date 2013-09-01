cd /d "%~dp0"
java -version
java -Xrs -cp bin -DshutdownWaitSecs=3 jp.seraphyware.win7frametest.JavaForWin7BadFrame
pause
