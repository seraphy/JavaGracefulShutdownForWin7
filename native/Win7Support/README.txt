[ビルド設定について]

このプロジェクトは、JavaのJNIを用いるため、

インクルードファイルのフォルダとして
%JAVA_HOME%\include
%JAVA_HOME%\include\win32

および、リンカの入力元フォルダとして
win32用とx64用に、それぞれの
%JAVA_HOME%\lib
の参照を設定する必要があります。

これらの設定は、"JavaConfig.props"プロパティファイルで、
JAVA_HOMEプロパティを設定することで変更できます。

"JavaConfig.props"を変更した場合は、ソリューションを開きなおしてください。
