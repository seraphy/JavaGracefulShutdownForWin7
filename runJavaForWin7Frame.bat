cd /d "%~dp0"
java -version
java -Xrs -Ddisable_win7_support=false -DshutdownWaitSecs=3 -cp target/JavaGracefulShutdownForWin7-0.0.1-SNAPSHOT.jar jp.seraphyware.win7frametest.JavaForWin7Frame
pause
