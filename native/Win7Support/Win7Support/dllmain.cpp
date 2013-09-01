// dllmain.cpp : DLL �A�v���P�[�V�����̃G���g�� �|�C���g���`���܂��B
#include "stdafx.h"

////
/// �v���Z�X���Ń��b�N�������邽�߂̃N���e�B�J���Z�N�V����
///
CRITICAL_SECTION criticalSection = {0};

///
/// DLL�̃G���g���|�C���g
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

