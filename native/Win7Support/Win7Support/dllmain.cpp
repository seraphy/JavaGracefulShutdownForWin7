// dllmain.cpp : DLL アプリケーションのエントリ ポイントを定義します。
#include "stdafx.h"

////
/// プロセス内でロックをかけるためのクリティカルセクション
///
CRITICAL_SECTION criticalSection = {0};

///
/// DLLのエントリポイント
///
BOOL APIENTRY DllMain(HMODULE hModule,
					  DWORD  ul_reason_for_call,
					  LPVOID lpReserved
					  )
{
	switch (ul_reason_for_call)
	{
	case DLL_THREAD_ATTACH:
		break;

	case DLL_THREAD_DETACH:
		break;

	case DLL_PROCESS_ATTACH:
		InitializeCriticalSection(&criticalSection);
		break;

	case DLL_PROCESS_DETACH:
		DeleteCriticalSection(&criticalSection);
		break;
	}
	return TRUE;
}

