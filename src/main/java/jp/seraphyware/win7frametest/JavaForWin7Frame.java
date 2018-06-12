package jp.seraphyware.win7frametest;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * JNIを用いてWindows7のネイティブAPIを通じて、 シャットダウンまたはリブート時にアプリケーションが
 * 正しくクリーンナップ処理できるように拡張した実装例.<br>
 */
public class JavaForWin7Frame extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * シャットダウンの待機時間(秒)
     */
    private static int SHUTDOWN_WAIT_SECS = 3;

    /**
     * メッセージ
     */
    private JLabel messageLabel = new JLabel();

    /**
     * シャットダウン処理のプログレスバー.
     */
    private JProgressBar progressBar;

    /**
     * 安全に終了するまでの遅延時間
     */
    private SpinnerNumberModel modelspnGracefullDelay;

    /**
     * シャットダウンフックの遅延時間
     */
    private SpinnerNumberModel modelShutdownHookDelay;

    /**
     * タスクトレイのアイコン
     */
    private TrayIcon trayIcon;

    /**
     * ネイティブライブラリをロードしたことを示すフラグ
     */
    private static boolean nativeLibLoaded;

    private static int ShutdownDownHookDelay = SHUTDOWN_WAIT_SECS;

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

        try {
            boolean nativeDisabled = Boolean.getBoolean("disable_win7_support");
            if (runningOnWindows7OrLater() && !nativeDisabled) {
                // OSがWindowsであり、システムプロパティで無効にされていなければ
                // ネイティブライブラリの読み込みを行う.
            	Win7Support.loadLibrary();

                nativeLibLoaded = true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * Windows7以降で動作しているか？
     *
     * @return Windows7以降であればtrue
     */
    private static boolean runningOnWindows7OrLater() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.indexOf("windows") != 0) {
            return false;
        }
        String osVersion = System.getProperty("os.version");
        BigDecimal ver = new BigDecimal(osVersion);
        return ver.compareTo(new BigDecimal("6.1")) >= 0;
    }

    /**
     * コンストラクタ
     */
    public JavaForWin7Frame() {
        try {
            // タイトル
            setTitle(getClass().getSimpleName());

            // ウィンドウの「閉じる」ボタンの動き
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    JavaForWin7Frame.this.windowClosing(e);
                }
            });

            // アイコンのロード
            String iconname = System.getProperty("iconname");
            if (iconname == null || iconname.trim().length() == 0) {
                iconname = "apple.png";
            }
            Image icon = null;
            URL iconURL = getClass().getResource(iconname);
            if (iconURL != null) {
                icon = ImageIO.read(iconURL);
            }

            if (icon != null) {
                // フレームのアイコン
                setIconImage(icon);

                // タスクトレイのアイコン
                trayIcon = new TrayIcon(icon);
                trayIcon.setImageAutoSize(true);
                SystemTray sysTray = SystemTray.getSystemTray();
                sysTray.add(trayIcon);

                // タスクアイコンのクリックアクション
                trayIcon.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 再表示する
                        JavaForWin7Frame.this.setVisible(true);
                        JavaForWin7Frame.this.toFront();
                        JavaForWin7Frame.this.repaint();
                    }
                });

                // タスクアイコンを表示する場合は、メインウィンドウは最小化で非表示にする.
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowIconified(WindowEvent e) {
                        if (!Boolean.parseBoolean(System.getProperty("disableMinimizeToTray"))) {
                            // 最小化で通知領域のアイコンだけにする.
                            JavaForWin7Frame.this.setVisible(false);
                        }
                    }
                });
            }

            // シャットダウン遅延中を表すプログレスバー
            progressBar = new JProgressBar();
            progressBar.setMinimum(0);
            progressBar.setMaximum(SHUTDOWN_WAIT_SECS * 10);
            progressBar.setValue(0);

            // パラメータ
            JPanel pnl = new JPanel();
            pnl.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            GridBagLayout gbl = new GridBagLayout();
            pnl.setLayout(gbl);
            GridBagConstraints gbc = new GridBagConstraints();

            JCheckBox checkEnable = new JCheckBox();
            Action actCheckEnable = new AbstractAction("Enable ShutdownDelayer") {
            	@Override
            	public void actionPerformed(ActionEvent e) {
            		if (checkEnable.isSelected()) {
            			enableShutdownDelayer();
            		} else {
            			disableShutdownDelayer();
            		}
            	}
            };
            checkEnable.setAction(actCheckEnable);

            gbc.gridy = 0;
            gbc.weightx = 1.;
            pnl.add(messageLabel, gbc);

            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.;
            pnl.add(checkEnable, gbc);

            gbc.gridy = 2;
            gbc.weightx = 0;
            pnl.add(new JLabel("Gracefull Delay (secs)"), gbc);

            gbc.gridy = 3;
            gbc.weightx = 0;
            pnl.add(new JLabel("Shutdown Hook Delay (secs)"), gbc);

            modelspnGracefullDelay = new SpinnerNumberModel(SHUTDOWN_WAIT_SECS, 0, 300, 1);
            JSpinner spnGracefullDelay = new JSpinner(modelspnGracefullDelay);

            gbc.gridy = 2;
            gbc.gridx = 1;
            gbc.weightx = 0;
            pnl.add(spnGracefullDelay, gbc);

            modelShutdownHookDelay = new SpinnerNumberModel(SHUTDOWN_WAIT_SECS, 0, 300, 1);
            JSpinner spnShutdownHookDelay = new JSpinner(modelShutdownHookDelay);
            modelShutdownHookDelay.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					ShutdownDownHookDelay = modelShutdownHookDelay.getNumber().intValue();
				}
			});

            gbc.gridy = 3;
            gbc.gridx = 1;
            gbc.weightx = 0;
            pnl.add(spnShutdownHookDelay, gbc);

            // レイアウト
            Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout());
            contentPane.add(pnl, BorderLayout.NORTH);
            contentPane.add(progressBar, BorderLayout.SOUTH);

            messageLabel.setText(nativeLibLoaded ? "Windows7サポートあり" : "Windows7サポートなし");
            pack();


            if (nativeLibLoaded) {
            	enableShutdownDelayer();
            }

        } catch (Exception ex) {
            dispose();
            throw new RuntimeException(ex);
        }
    }

    private boolean enableShutdownDelayer;

    private void enableShutdownDelayer() {
    	if (!enableShutdownDelayer) {
            // シャットダウンの遅延を設定する.
            // ※ シャットダウンの遅延はネイティブウィンドウに関連づけられており、
            // ウィンドウが破棄されれば解除されることに注意.
            Win7Support.shutdownBlockReasonCreate(this);
            enableShutdownDelayer = true;
            System.out.println("enableShutdownDelayer.");
    	}
    }

    private void disableShutdownDelayer() {
    	if (enableShutdownDelayer) {
            // シャットダウンの遅延を解除する.
            // Win7Support.shutdownBlockReasonDestroy(JavaForWin7Frame.this);
            // ※ 明示的に解除しなくてもウィンドウが破棄されると自動的に解除となる.
        	Win7Support.shutdownBlockReasonDestroy(this);
            System.out.println("disableShutdownDelayer.");
            enableShutdownDelayer = false;
    	}
    }

    /**
     * シャットダウンを遅延させるタイマー
     */
    private Timer timer;

    /**
     * ウィンドウの「閉じる」アクション.
     *
     * @param e
     */
    protected void windowClosing(WindowEvent e) {
        // タスクトレイアイコンがあれば解除する.
        if (trayIcon != null) {
            SystemTray sysTray = SystemTray.getSystemTray();
            sysTray.remove(trayIcon);
            trayIcon = null;
        }

        if (timer == null) {
            System.out.println("wait for graceful shutdown.");
            messageLabel.setText("終了処理中です");
            int delay = modelspnGracefullDelay.getNumber().intValue();
            System.out.println("Graceful shutdown delay=" + delay);
            progressBar.setMaximum(delay * 10);
            progressBar.setValue(progressBar.getMaximum());

            // タイマーで0.1秒ごとにカウントダウンする.
            // プログレスバーが0になるまで終了処理を遅延させる.
            timer = new Timer(100, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int v = progressBar.getValue();
                    System.out.println("countdown=" + v);
                    v = v - 1;

                    progressBar.setValue(v);
                    if (v <= progressBar.getMinimum()) {
                        // プログレスバーが最小値まで到達したら終了する.
                        timer.stop();

                        System.out.println("done!");
                        System.out.flush();

                        if (nativeLibLoaded && enableShutdownDelayer) {
                        	disableShutdownDelayer();
                        }

                        JavaForWin7Frame.this.dispose();
                    }
                }
            });
            timer.start();
        }
    }

    /**
     * アプリケーションのタスクバーのグループを決める識別名を設定する.
     *
     * @param appname アプリケーション識別名、nullの場合は、このクラス名を使用する.
     */
    private static void initAppUserModelId(String appname) {
        if (appname == null || appname.trim().length() == 0) {
            appname = JavaForWin7Frame.class.getName();
        }
        System.out.println("appname=" + appname);

        if (nativeLibLoaded) {
            // アプリケーション識別名を設定する.
            Win7Support.setCurrentProcessExplicitAppUserModelID(appname);
        }
    }

    /**
     * シャットダウンフックを設定する.
     */
    private static void initShutdownHook() {
        // ※ 本プログラムではJFrameをdisposeするときにシャットダウン遅延を解除
        // しているため、JFrameのdispose直後にアプリが終了させられる可能性があり、
        // これが呼ばれる保証はない。(もしくは途中で強制終了される可能性がある.)
        Runtime rt = Runtime.getRuntime();
        rt.addShutdownHook(new Thread() {
            @Override
            public void run() {
                int delay = JavaForWin7Frame.ShutdownDownHookDelay;
                System.out.println("Shutdown Delay: " + delay);
                int mx = delay * 10;
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
                System.out.println("done!!.");
                System.out.flush();
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
        // アプリケーションのタスクバーのグループを決める識別名を設定する.
        // (システムプロパティによりアイコングループを無効にできる)
        if (!Boolean.getBoolean("ignore_appname")) {
            initAppUserModelId(args.length >= 1 ? args[0] : null);
        }

        // シャットダウンを遅延する秒数を設定する.
        String shutdownWaitSecs = System.getProperty("shutdownWaitSecs");
        if (shutdownWaitSecs != null && shutdownWaitSecs.trim().length() > 0) {
            SHUTDOWN_WAIT_SECS = Integer.parseInt(shutdownWaitSecs.trim());
        }

        // シャットダウンフックを設定.
        // (Windows7ではシャットダウン、リブート時には機能しない)
        initShutdownHook();

        // L&Fをシステムの標準に設定
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // メインウィンドウの作成・表示
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JavaForWin7Frame app = new JavaForWin7Frame();
                app.setLocationByPlatform(true);
                app.setVisible(true);
            }
        });
    }
}
