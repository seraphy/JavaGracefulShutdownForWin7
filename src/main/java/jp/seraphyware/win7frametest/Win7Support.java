package jp.seraphyware.win7frametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.swing.JFrame;

/**
 * Windows7用のネイティブ設定のためのクラス.
 */
public class Win7Support {

    /**
     * ネイティブライブラリをロードしたことを示すフラグ
     */
	private static boolean nativeLibLoaded;

	/**
	 * ライブラリ名
	 */
	private static final String DLL_NAME = "Win7Support.dll";

	/**
	 * jarファイルに格納されたdllをx86/x64の実行環境にあわせて取り出して
	 * テンポラリディレクトリ上にdllファイルとしてコピーしてロードする。
	 * また、dllのロードに先立って、"jawt.dll" をロードする。
	 * @throws IOException
	 */
	public static void loadLibrary() throws IOException {
		if (!nativeLibLoaded) {
            // JRE付属のJAWT.DLL Nativeライブラリをロードする.
            // 本アプリ用の"Win7Support.dll"はJAWT.DLLとリンクする必要があるため、
            // JAWT.DLLの所在を把握しているjava側から事前にロードしておくと良い.
            System.loadLibrary("jawt");

            // 本アプリ用の"Win7Support.dll" Nativeライブラリのロード
            // このアプリのクラスが格納されているベースフォルダと同じ位置にあるNativeフォルダ下に
            // 本アプリ用のdllがあると想定している.
            // 実行環境が32/64ビットであるかに応じて対応するDLLのロードを切り替えている.
            String resourceName;
            if (System.getProperty("os.arch").indexOf("64") >= 0) {
            	resourceName = "dll/x64/" + DLL_NAME;
            } else {
            	resourceName = "dll/x86/" + DLL_NAME;
            }
            System.out.println("resourceName=" + resourceName);

            MessageDigest sha1;
            try {
            	sha1 = MessageDigest.getInstance("SHA1");

            } catch (NoSuchAlgorithmException ex) {
            	throw new RuntimeException(ex);
            }

            long size = 0;
            try (InputStream is = Win7Support.class.getClassLoader().getResourceAsStream(resourceName);
            		ReadableByteChannel ic = Channels.newChannel(is)) {
            	ByteBuffer buf = ByteBuffer.allocate(4096);
            	int rd;
            	while ((rd = ic.read(buf)) >= 0) {
            		buf.flip();
            		size += rd;
            		sha1.update(buf);
            		buf.clear();
            	}
            }

            byte[] hash = sha1.digest();
            String sha1Str = Base64.getUrlEncoder().encodeToString(hash);

            Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
            Path dllPath = tmpDir.resolve("jp.seraphyware.example").resolve(sha1Str).resolve(DLL_NAME);
            Files.createDirectories(dllPath.getParent());
            System.out.println("dllPath=" + dllPath);

            // 同一ハッシュ値、同一サイズの既存のDLLがすでにテンポラリに展開されている場合は改めて展開はしない。
            if (!Files.exists(dllPath) && Files.size(dllPath) == size) {
                try (InputStream is = Win7Support.class.getClassLoader().getResourceAsStream(resourceName);
                		FileChannel fc = (FileChannel) Files.newByteChannel(dllPath,
                				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                				StandardOpenOption.WRITE)) {
                	fc.transferFrom(Channels.newChannel(is), 0, size);
                }
            }

            System.load(dllPath.toString());

			nativeLibLoaded = true;
            System.out.println("nativeLibLoaded=" + nativeLibLoaded);
		}
	}

    /**
     * タスクバーでグルーピングするためのアプリケーション識別名を設定する.
     *
     * @param appname アプリケーション識別名
     */
    public static native void setCurrentProcessExplicitAppUserModelID(String appname);

    /**
     * タスクバーでグルーピングするためのアプリケーション識別名を取得する.
     *
     * @return アプリケーション識別名、未設定ならばnull
     */
    public static native String getCurrentProcessExplicitAppUserModelID();

    /**
     * シャットダウンの制限を設定する.
     *
     * @param frame JFrameのインスタンス
     */
    public static native void shutdownBlockReasonCreate(JFrame frame);

    /**
     * シャットダウンの制限を解除する.<br>
     *
     * @param frame JFrameのインスタンス
     */
    public static native void shutdownBlockReasonDestroy(JFrame frame);
}
