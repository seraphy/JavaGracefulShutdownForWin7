package jp.seraphyware.win7frametest;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 通常のJFrameと、シャットダウンフックで作成したクリーンナップ処理の例.<br>
 * Windows XPまでならば正常に動作するが、Vista以降の場合はログオフ、リブート、シャットダウン時には
 * クリーンナップが完了するまえに強制終了させられる.<br>
 */
public class JavaForWin7BadFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * シャットダウンの待機時間(秒)
     */
    private static int SHUTDOWN_WAIT_SECS = 3;

    /**
     * Staticイニシャライザ
     */
    static {
        try {
            // 標準出力をファイルに送る.(ログ)
            OutputStream writer = new FileOutputStream("console.log");
            PrintStream pw = new PrintStream(writer);
            System.setOut(pw);
            System.setErr(pw);

            System.out.println("now=" + new Date());

        } catch (Exception ex) {
            ex.printStackTrace();
            // 無視する.
        }
    }

    /**
     * コンストラクタ
     */
    public JavaForWin7BadFrame() {
        setTitle(getClass().getSimpleName());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * ウィンドウが破棄される場合に呼び出される.
     */
    @Override
    public void dispose() {
        // ※ Windows7/8では、シャットダウンまたはリブートの場合は
        // JFrame#dispose()が呼ばれる前に終了してしまう.
        System.out.println("windowDisposing");
        System.out.flush();
        super.dispose();
    }

    /**
     * シャットダウンフックを設定する.
     */
    private static void initShutdownHook() {
        Runtime rt = Runtime.getRuntime();
        rt.addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Windows7/8ではログオフ、シャットダウン、リブート時は強制終了させられる.
                // クリーンナップ処理が完了するまえにプロセスが終了させられる.
                int mx = SHUTDOWN_WAIT_SECS * 10;
                for (int idx = 0; idx < mx; idx++) {
                    System.out.println("called shutdown hook! " +
                            (idx + 1) + "/" + mx);
                    System.out.flush();
                    try {
                        Thread.sleep(100);

                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        });
    }

    /**
     * エントリポイント
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // シャットダウンを遅延する秒数を設定する.
        String shutdownWaitSecs = System.getProperty("shutdownWaitSecs");
        if (shutdownWaitSecs != null && shutdownWaitSecs.trim().length() > 0) {
            SHUTDOWN_WAIT_SECS = Integer.parseInt(shutdownWaitSecs.trim());
        }

        // シャットダウンフックを設定.
        initShutdownHook();

        // L&Fをシステムの標準に設定
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // メインウィンドウの作成・表示
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JavaForWin7BadFrame app = new JavaForWin7BadFrame();
                app.setLocationByPlatform(true);
                app.setVisible(true);
            }
        });
    }
}
