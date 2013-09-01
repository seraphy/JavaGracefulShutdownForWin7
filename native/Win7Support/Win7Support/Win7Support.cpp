// Win7Support.cpp : DLL アプリケーション用にエクスポートされる関数を定義します。
//

#include "stdafx.h"

#include <jawt_md.h>

#include "jp_seraphyware_win7frametest_Win7Support.h"

#include <vector>
#include <map>
#include <Shobjidl.h>

///
/// プロセス内でロックをかけるためのクリティカルセクション
///
extern CRITICAL_SECTION criticalSection;

///
/// ウィンドウプロシージャの型定義
///
typedef LRESULT (CALLBACK *P_WindowProc)(
	_In_  HWND hwnd,
	_In_  UINT uMsg,
	_In_  WPARAM wParam,
	_In_  LPARAM lParam
	);

namespace
{
	///
	/// シャットダウンの理由
	///
	LPCWSTR SHUTDOWN_REASON = L"wait for graceful shutdown";

	///
	/// サブクラス化する前のオリジナルのフレームのプロシージャ
	///
	std::map<HWND, P_WindowProc> originalWndProcMap;

	///
	/// ウィンドウハンドルに対するオリジナルのウィンドウプロシージャを取得する.
	///
	P_WindowProc findOriginalWndProc(HWND hWnd)
	{
		P_WindowProc pOriginalWndProc = NULL;
		std::map<HWND, P_WindowProc>::iterator pEntry = originalWndProcMap.find(hWnd);
		if (pEntry != originalWndProcMap.end()) {
			pOriginalWndProc = pEntry->second;
		}
		return pOriginalWndProc;
	}

	///
	/// サブクラス化するための独自のウィンドウプロシージャ.
	/// JFrameの既存のウィンドウプロシージャをフックし、
	/// WM_QUERYENDSESSION, WM_ENDSESSIONでウィンドウを閉じるようにする.
	///
	LRESULT CALLBACK HookedWindowProc(
		_In_  HWND hWnd,
		_In_  UINT message,
		_In_  WPARAM wParam,
		_In_  LPARAM lParam
		) {
			EnterCriticalSection(&criticalSection);
			P_WindowProc pOriginalWndProc = findOriginalWndProc(hWnd);
			LeaveCriticalSection(&criticalSection);

			switch (message)
			{
			case WM_QUERYENDSESSION:
				// シャットダウン、リブートの要求があった場合の問い合わせで
				// ウィンドウにクローズを指示する.
				PostMessage(hWnd, WM_CLOSE, 0, 0);

				// シャットダウンの制限
				// ※ ウィンドウが非表示になっている場合、このメッセージ中で
				// 改めて呼び出さないと無視されてしまう.
				// ※ このメッセージ中で呼び出さないとREASONの表示がシステムデフォルトになる.
				ShutdownBlockReasonCreate(hWnd, SHUTDOWN_REASON);
				return 0;

			case WM_ENDSESSION:
				// シャットダウン、リブートの実施の通知で、
				// ウィンドウにクローズを指示する.
				PostMessage(hWnd, WM_CLOSE, 0, 0);
				return 0;

			case WM_DESTROY:
				// ウィンドウを破棄するためマップから除去しサブクラス化を解除する.
				EnterCriticalSection(&criticalSection);
				originalWndProcMap.erase(hWnd);
				if (pOriginalWndProc) {
					SetWindowLongPtr(hWnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(pOriginalWndProc));
				}
				LeaveCriticalSection(&criticalSection);
				break;
			}

			if (pOriginalWndProc) {
				// それ以外のメッセージは、もとのウィンドウプロシージャの処理を行う.
				return (*pOriginalWndProc)(hWnd, message, wParam, lParam);
			}
			// フォールバック用
			return DefWindowProc(hWnd, message, wParam, lParam);
	}

	///
	/// JFrameのウィンドウハンドルを取得する.
	/// ("jawt.lib"へのリンクが必要.)
	///
	HWND GetHwndFromJFrame(JNIEnv *env, jclass cls, jobject jframe)
	{
		JAWT awt = {0};
		awt.version = JAWT_VERSION_1_4;
		if (JAWT_GetAWT(env, &awt) == JNI_FALSE) {
			return NULL;
		}

		JAWT_DrawingSurface* ds = awt.GetDrawingSurface(env, jframe);
		if (ds == NULL) {
			return NULL;
		}

		jint lock = ds->Lock(ds);
		if ((lock & JAWT_LOCK_ERROR) != 0) {
			awt.FreeDrawingSurface(ds);
			return NULL;
		}

		JAWT_DrawingSurfaceInfo* dsi = ds->GetDrawingSurfaceInfo(ds);
		if (dsi == NULL) {
			ds->Unlock(ds);
			awt.FreeDrawingSurface(ds);
			return NULL;
		}
		JAWT_Win32DrawingSurfaceInfo* dsi_win =
			reinterpret_cast<JAWT_Win32DrawingSurfaceInfo*>(dsi->platformInfo);

		HWND hWnd = dsi_win->hwnd;

		ds->FreeDrawingSurfaceInfo(dsi);
		ds->Unlock(ds);
		awt.FreeDrawingSurface(ds);

		return hWnd;
	}
}

///
/// タスクバーでグルーピングするためのアプリケーション識別名を設定する.
///
JNIEXPORT void JNICALL Java_jp_seraphyware_win7frametest_Win7Support_setCurrentProcessExplicitAppUserModelID
	(JNIEnv *env, jclass cls, jstring appname)
{
	jsize len = env->GetStringLength(appname);
	jboolean copied = JNI_FALSE;
	const jchar *pAppName = env->GetStringChars(appname, &copied);

	std::vector<jchar> buf(len + 1); // Zero Terminate
	jchar *pBuf = &buf[0];
	memcpy(pBuf, pAppName, sizeof(jchar) * len);

	LPCWSTR cszAppName = reinterpret_cast<LPCWSTR>(pBuf);
	SetCurrentProcessExplicitAppUserModelID(cszAppName);

	if (copied) {
		env->ReleaseStringChars(appname, pAppName);
	}
}

///
/// タスクバーでグルーピングするためのアプリケーション識別名を取得する.
///
JNIEXPORT jstring JNICALL Java_jp_seraphyware_win7frametest_Win7Support_getCurrentProcessExplicitAppUserModelID
	(JNIEnv *env, jclass cls)
{
	jstring ret = NULL;

	LPWSTR pBuf = NULL;
	HRESULT retcode = GetCurrentProcessExplicitAppUserModelID(&pBuf);
	if (SUCCEEDED(retcode)) {
		int len = lstrlenW(pBuf);
		ret = env->NewString(reinterpret_cast<jchar *>(pBuf), len);

		CoTaskMemFree(pBuf);
	}

	return ret;
}

///
/// シャットダウンの制限を設定する.
///
JNIEXPORT void JNICALL Java_jp_seraphyware_win7frametest_Win7Support_shutdownBlockReasonCreate
	(JNIEnv *env, jclass cls, jobject jframe)
{
	// JFrameのウィンドウハンドルの取得
	HWND hWnd = GetHwndFromJFrame(env, cls, jframe);
	if (hWnd == NULL) {
		return;
	}

#ifdef _DEBUG
	// シャットダウンされる順序を指定する.
	// テストしやすいように最初の方でシャットダウンする.
	SetProcessShutdownParameters(0x4ff, 0);
#endif

	// シャットダウンの制限
	ShutdownBlockReasonCreate(hWnd, SHUTDOWN_REASON);

	// まだウィンドウプロシージャをサブクラス化していなければ、
	// 現在のウィンドウプロシージャを取得し、サブクラス化できるようにする.
	EnterCriticalSection(&criticalSection);
	P_WindowProc pOriginalWndProc = findOriginalWndProc(hWnd);
	if (pOriginalWndProc == NULL) {
		P_WindowProc pWndProc = reinterpret_cast<P_WindowProc>(GetWindowLongPtr(hWnd, GWLP_WNDPROC));
		if (pWndProc != NULL && pWndProc != HookedWindowProc) {
			// ウィンドウプロシージャが見つからないか、すでにサブクラス化済みの場合は置換しない.
			// そうでなければ独自のウィンドウプロシージャで置き換えてサブクラス化する.
			originalWndProcMap.insert(std::make_pair(hWnd, pWndProc));
			SetWindowLongPtr(hWnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(HookedWindowProc));
		}
	}
	LeaveCriticalSection(&criticalSection);
}

///
/// シャットダウンの制限を解除する.
///
JNIEXPORT void JNICALL Java_jp_seraphyware_win7frametest_Win7Support_shutdownBlockReasonDestroy
	(JNIEnv *env, jclass cls, jobject jframe)
{
	// JFrameのウィンドウハンドルの取得
	HWND hWnd = GetHwndFromJFrame(env, cls, jframe);
	if (hWnd == NULL) {
		return;
	}

	// シャットダウンの制限解除
	ShutdownBlockReasonDestroy(hWnd);
}
