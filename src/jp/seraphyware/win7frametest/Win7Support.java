package jp.seraphyware.win7frametest;

import javax.swing.JFrame;

/**
 * Windows7用のネイティブ設定のためのクラス.
 */
public class Win7Support {

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
