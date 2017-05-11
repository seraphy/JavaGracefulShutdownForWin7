JavaアプリのWindows7における終了処理について
=======================================

## 概要

**Windows Vista以降**、システムのログオフ、リブート、シャットダウンを行う場合、
起動中のアプリケーションは終了通知が送られた際に、ShutdownBlockReasonCreate関数を呼び出さないかぎり、
1秒程度ほどの猶予しか与えられなくなっている。

上記APIを呼び出さずに1秒程度を超えるとプロセスは強制終了される。

本アプリでは、JNIを用いてShutdownBlockReasonCreate関数を呼び出すことにより、
ログオフ、リブート、シャットダウンの要求時でもアプリケーションの終了処理が完了するように拡張するものである。

## 使い方と仕組み

シャットダウン時に強制終了されないように、ネイティブ関数である **ShutdownBlockReasonCreate関数** を呼び出すための
ネイティブ関数 **Win7Support#shutdownBlockReasonCreate** を作成し、Javaアプリケーション上から、これを呼び出す。

ShutdownBlockReasonCreate関数は表示中のメインウィンドウへのウィンドウハンドルが必要であるため、
JNIのライブラリであるJAWT.DLLを経由してウィンドウハンドルを取得し、これをShutdownBlockReasonCreateに渡す。


**JAWT.DLL**はJRE付属のライブラリであるが必ずしもロードされているとは限らないため、
本アプリ用のネイティブライブラリのDLLをロードする前に事前に **"LoadLibrary"** によってロードしておく。

これはJava内からであればJAWT.DLLの場所はわかっており、環境変数PATHなどを操作する必要がないこと。
また、同名のDLLは一度ロードすれば二度目はロードされずに済む、というWin32の性質によるものである。


シャットダウンを遅延させるためには、シャットダウン開始時にシステムからアプリケーションに **WM_QUERYENDSESSIONメッセージ** が
送信されたとき、このメッセージに対する応答としてShutdownBlockReasonCreateを呼び出し、
シャットダウンを遅延することを示すFALSEの応答を返すのか良い。

このため、本アプリケーションは、 **Win7Support#shutdownBlockReasonCreate()** の呼び出し時に、
対象となるJFrameのネイティブのウィンドウプロシージャをサブクラス化(WndProcのカスケード化)し、
WM_QUERYENDSESSIONメッセージの処理をオーバーライドしている。

なお、 **ShutdownBlockReasonDestroy関数** によってシャットダウン遅延は解除される。

ただし、ウィンドウが破棄されれば、自動的に解除されたとみなされるため、終了処理時には呼び出しを忘れたとしても
うまくシャットダウンしてくれる。


## 注意点

本アプリで使用するネイティブライブラリは、JFrameのネイティブウィンドウのウィンドウプロシージャのメッセージ処理をオーバーライドするため、
ウィンドウプロシージャのサブクラス化という手法をとっている。

サブクラス化とは、システムからのウィンドウメッセージを処理するウィンドウプロシージャへのアドレス **GWPL_WNDPROC** を、
それぞれのウィンドウが保持しているため、これを書き換えてメッセージ処理を別のウィンドウプロシージャに転送することである。

転送先では、カスタマイズが必要なメッセージの処理を行ったあと、それ以外のメッセージを従来どおりに処理するため、
元のプロシージャを呼び出すようにチェイン化すれば、あたかもウィンドウプロシージャがオーバーライド(サブクラス化)されているかのように振る舞うことになる。

これはWindowsの標準コントロールの挙動をアプリケーション側で変更したい場合などに使われる、Windowsでの昔からの"公式"の手法でもある。

しかし、サブクラス化される側の挙動に強く影響されるため、JFrameのサブクラス化は実装依存であり、将来のJREの実装の変更によって動作しなくなる可能性がある。

本アプリケーション例のように、Javaアプリのネイティブの挙動をカスタマイズするような必要性がある場合には、
動作するJREのバージョンを確認済みのものとするため、アプリケーションにJREをバンドルし、そのJREのみを使用するのが望ましいといえる。

なお、確認したかぎりでは、Sunの実装であるJRE6でも、Oracleの実装であるJRE7(およびJRE8-ea版)でも、
本アプリケーションは、うまく機能しているため、それほどシビアではなさそうではある。

## 本アプリのネイティブライブラリのビルド方法

本アプリのネイティブライブラリは、 **[Visual Studio 2012 Express for Desktop](http://www.microsoft.com/visualstudio/jpn/products/visual-studio-express-for-windows-desktop)** でビルドできるようになっている。

ビルドには、jawt.libとjniの各種ヘッダを参照するため、JDKのJAVA_HOMEを設定する必要がある。

これは **"JavaConfig.props"** ファイルで指定する。

32ビットと64ビットの、それぞれのJDKへの位置を指定する必要がある。

このファイルを書き換えた場合にはソリューションの再起動が必要である。


## 参考文献/URL

Javaアプリでlogoff時の終了処理を遅延させる例

- [OKIソフトウェア::25. Windows 7 のコンソール・アプリで logoff 時に長い処理を行うには](http://www.oki-osk.jp/esc/cyg/cygwin-25.html)

ShutdownBlockReasonCreate関数の使いかた例

- [DOBON.NETプログラミング掲示板過去ログ::スタートメニューの電源ボタン押下のイベント取得方法](http://dobon.net/vb/bbs/log3-36/22254.html)

## ライセンス

本サンプルのライセンスは、オープンソースのMITライセンスとします。
