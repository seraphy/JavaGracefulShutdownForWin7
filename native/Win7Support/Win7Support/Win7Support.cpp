// Win7Support.cpp : DLL �A�v���P�[�V�����p�ɃG�N�X�|�[�g�����֐����`���܂��B
//

#include "stdafx.h"

#include <jawt_md.h>

#include "jp_seraphyware_win7frametest_Win7Support.h"

#include <vector>
#include <map>
#include <Shobjidl.h>

///
/// �v���Z�X���Ń��b�N�������邽�߂̃N���e�B�J���Z�N�V����
///
extern CRITICAL_SECTION criticalSection;

///
/// �E�B���h�E�v���V�[�W���̌^��`
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
	/// �V���b�g�_�E���̗��R
	///
	LPCWSTR SHUTDOWN_REASON = L"wait for graceful shutdown";

	///
	/// �T�u�N���X������O�̃I���W�i���̃t���[���̃v���V�[�W��
	///
	std::map<HWND, P_WindowProc> originalWndProcMap;

	///
	/// �E�B���h�E�n���h���ɑ΂���I���W�i���̃E�B���h�E�v���V�[�W�����擾����.
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
	/// �T�u�N���X�����邽�߂̓Ǝ��̃E�B���h�E�v���V�[�W��.
	/// JFrame�̊����̃E�B���h�E�v���V�[�W�����t�b�N���A
	/// WM_QUERYENDSESSION, WM_ENDSESSION�ŃE�B���h�E�����悤�ɂ���.
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
				// �V���b�g�_�E���A���u�[�g�̗v�����������ꍇ�̖₢���킹��
				// �E�B���h�E�ɃN���[�Y���w������.
				PostMessage(hWnd, WM_CLOSE, 0, 0);

				// �V���b�g�_�E���̐���
				// �� �E�B���h�E����\���ɂȂ��Ă���ꍇ�A���̃��b�Z�[�W����
				// ���߂ČĂяo���Ȃ��Ɩ�������Ă��܂�.
				// �� ���̃��b�Z�[�W���ŌĂяo���Ȃ���REASON�̕\�����V�X�e���f�t�H���g�ɂȂ�.
				ShutdownBlockReasonCreate(hWnd, SHUTDOWN_REASON);
				return 0;

			case WM_ENDSESSION:
				// �V���b�g�_�E���A���u�[�g�̎��{�̒ʒm�ŁA
				// �E�B���h�E�ɃN���[�Y���w������.
				PostMessage(hWnd, WM_CLOSE, 0, 0);
				return 0;

			case WM_DESTROY:
				// �E�B���h�E��j�����邽�߃}�b�v���珜�����T�u�N���X������������.
				EnterCriticalSection(&criticalSection);
				originalWndProcMap.erase(hWnd);
				if (pOriginalWndProc) {
					SetWindowLongPtr(hWnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(pOriginalWndProc));
				}
				LeaveCriticalSection(&criticalSection);
				break;
			}

			if (pOriginalWndProc) {
				// ����ȊO�̃��b�Z�[�W�́A���Ƃ̃E�B���h�E�v���V�[�W���̏������s��.
				return (*pOriginalWndProc)(hWnd, message, wParam, lParam);
			}
			// �t�H�[���o�b�N�p
			return DefWindowProc(hWnd, message, wParam, lParam);
	}

	///
	/// JFrame�̃E�B���h�E�n���h�����擾����.
	/// ("jawt.lib"�ւ̃����N���K�v.)
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
/// �^�X�N�o�[�ŃO���[�s���O���邽�߂̃A�v���P�[�V�������ʖ���ݒ肷��.
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
/// �^�X�N�o�[�ŃO���[�s���O���邽�߂̃A�v���P�[�V�������ʖ����擾����.
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
/// �V���b�g�_�E���̐�����ݒ肷��.
///
JNIEXPORT void JNICALL Java_jp_seraphyware_win7frametest_Win7Support_shutdownBlockReasonCreate
	(JNIEnv *env, jclass cls, jobject jframe)
{
	// JFrame�̃E�B���h�E�n���h���̎擾
	HWND hWnd = GetHwndFromJFrame(env, cls, jframe);
	if (hWnd == NULL) {
		return;
	}

#ifdef _DEBUG
	// �V���b�g�_�E������鏇�����w�肷��.
	// �e�X�g���₷���悤�ɍŏ��̕��ŃV���b�g�_�E������.
	SetProcessShutdownParameters(0x4ff, 0);
#endif

	// �V���b�g�_�E���̐���
	ShutdownBlockReasonCreate(hWnd, SHUTDOWN_REASON);

	// �܂��E�B���h�E�v���V�[�W�����T�u�N���X�����Ă��Ȃ���΁A
	// ���݂̃E�B���h�E�v���V�[�W�����擾���A�T�u�N���X���ł���悤�ɂ���.
	EnterCriticalSection(&criticalSection);
	P_WindowProc pOriginalWndProc = findOriginalWndProc(hWnd);
	if (pOriginalWndProc == NULL) {
		P_WindowProc pWndProc = reinterpret_cast<P_WindowProc>(GetWindowLongPtr(hWnd, GWLP_WNDPROC));
		if (pWndProc != NULL && pWndProc != HookedWindowProc) {
			// �E�B���h�E�v���V�[�W����������Ȃ����A���łɃT�u�N���X���ς݂̏ꍇ�͒u�����Ȃ�.
			// �����łȂ���ΓƎ��̃E�B���h�E�v���V�[�W���Œu�������ăT�u�N���X������.
			originalWndProcMap.insert(std::make_pair(hWnd, pWndProc));
			SetWindowLongPtr(hWnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(HookedWindowProc));
		}
	}
	LeaveCriticalSection(&criticalSection);
}

///
/// �V���b�g�_�E���̐�������������.
///
JNIEXPORT void JNICALL Java_jp_seraphyware_win7frametest_Win7Support_shutdownBlockReasonDestroy
	(JNIEnv *env, jclass cls, jobject jframe)
{
	// JFrame�̃E�B���h�E�n���h���̎擾
	HWND hWnd = GetHwndFromJFrame(env, cls, jframe);
	if (hWnd == NULL) {
		return;
	}

	// �V���b�g�_�E���̐�������
	ShutdownBlockReasonDestroy(hWnd);
}
