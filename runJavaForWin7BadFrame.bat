cd /d "%~dp0"
java -version
java -Xrs -cp target/JavaGracefulShutdownForWin7-0.0.1-SNAPSHOT.jar -DshutdownWaitSecs=3 jp.seraphyware.win7frametest.JavaForWin7BadFrame
pause
