/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.
 * Patented and other Patents pending.
 * All trademarks are property of their respective owners.
 * SideTap, SideSafe, SideKey, The Connected Smart Card,
 * CSC are trademarks of Tyfone, Inc.
 * For questions visit: www.tyfone.com
 */

// SD2SIM.cpp : Defines the entry point for the DLL application.
//
//#include "stdafx.h"
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>
#include <sys/stat.h>
#include "SD2SIM.h"
#include "stdio.h"
#include "stdbool.h"

#include "internal/types.h"
#include "internal/GenDef.h"
#include "internal/CardControl.h"
#include "cscdirectio.h"

#include "LowLevel.h"

#include <android/log.h>
#define TAG "SD2SIM"

#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__))
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__))
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO   , TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN   , TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR  , TAG, __VA_ARGS__))

#if _DEBUG
#include "conio.h"
#define DbgOut(_x_)		cprintf _x_
#else
#define DbgOut(_x_)
#endif

U08 dummyPattern[] = { 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
		0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50,
		0x51, 0x52, 0x53, 0x54, 0x55, };

const char* TUP_HEADER =
		"9CDA58E34BE5391D50EDA976AA2D0E124A5BC495511C749381F97A1CEEBD05D5";

const U08 abHwSig[0x10] = { 0x65, 0x76, 0x67, 0x79, 0x82, 0x95, 0x84, 0x79,
		0x95, 0x83, 0x73, 0x77, 0x95, 0x67, 0x77, 0x68 };
const U08 abRspSig[0x10] = { 'S', 'D', '_', 'T', 'O', '_', 'S', 'I', 'M', '_',
		'B', 'L', 'O', 'C', 'K', '_' };
const U08 abCmdFwSig[0x10] = { 0x98, 0x62, 0x97, 0x88, 0x03, 0x72, 0x13, 0x42,
		0x84, 0x27, 0x93, 0xC2, 0x2F, 0x33, 0xD2, 0x8E };
const U08 abRspFwSig[0x10] = { 0x7F, 0x22, 0x82, 0xB3, 0xCC, 0x82, 0x12, 0xD8,
		0x62, 0x3D, 0x72, 0x14, 0x32, 0xE8, 0x2D, 0x77 };

U16 F_factor[16] = { 372, 372, 558, 744, 1116, 1488, 1860, 99, 99, 512, 768,
		1024, 1536, 2048, 99, 99 };
U08 D_factor[16] =
		{ 99, 1, 2, 4, 8, 16, 32, 64, 12, 20, 99, 99, 99, 99, 99, 99 }; //new Di support 64
U08 abRandNum[4];
U08 g_bA = 0;
U08 g_bB = 0xFF;

U08 pollBytes[] = { 0x01, 0x02, 0x03, 0x04, 0x05 };

char abCmdFile[MAX_PATH_SIZE] = { 0 };
char pollFile[MAX_PATH_SIZE] = { 0 };

U08 abATR[64];
U16 atrLen = 0;

//BOOL  SdWriteFile(U08 *pbData, U16 y);
//BOOL  SdReadFile(U08 *pbData, U16 *y, U08 blFirstTime);
BOOL Get_Alcor_Disk(const char* diskLetter);
U08 CalLRC(U08 *pbData, U16 wLen);

U08 AmIsCardPresent() {
	if (isFileExists(abCmdFile) == E_SUCCESS)
		return TRUE;
	else
		return FALSE;
}

U08 AmConnect(const char* diskLetter) {
	DbgOut(("  ================     AmConnect()    =============\n"));
	if (Get_Alcor_Disk(diskLetter) == FALSE)
		return 0x01;
	return E_SUCCESS;
}

U08 AmDisconnect() {
	//closeFiledescriptor();
	closeTyfoneFile();
	return E_SUCCESS;
}

U08 AmPowerOn(U16 wmProtocol) {
	DbgOut(("================   AmPowerOn(%04X)  ==============\n",wmProtocol));
	LOGI("================   AmPowerOn ==============\n");
	U08 bmVolSel;
	U08 x;
	U08 bStatus;
	U08 bmVoltage;
	U08 bResetType;

	bResetType = COLD_RESET_TYPE;
	bStatus = E_UNKNOWN_ERR;

	bmVoltage = SC_3_VOLTAGE;

	AmSCEnable();
	for (x = 0; x < SC_VOLTAGE_NUM; x++) {
		bmVolSel = bmVoltage & (1 << x);
		if (bmVolSel == 0) {
			continue;
		}

		while (bResetType < FAILED_RESET_TYPE) {

			bStatus = SimPowerOn(bmVolSel, bResetType, wmProtocol);

			if (bStatus & E_ICC_REJECT) {

				break;
			}

			if (bStatus == E_SUCCESS) {

				return E_SUCCESS;
			}

			bResetType++;
			if (bStatus & E_ATR_REJECT) {

				continue;
			} else if (bStatus & E_ICC_REJECT) {

				break;
			}
		}
		bResetType = COLD_RESET_TYPE;
		SimPowerOff();
	}

	return bStatus;

}

U08 AmPowerOff() {
	return SimPowerOff();

}

U08 AmTransmit(U08 *pbApduCmd, U16 wApduCmdLen, U08 *pbApduRsp, U16 wApduRspLen,
U16 *pwApduRspReplyLen) {
	U08 bStatus = E_SUCCESS;

	bStatus = SimApduExchange(pbApduCmd, wApduCmdLen, pbApduRsp, wApduRspLen,
			pwApduRspReplyLen);
	return bStatus;
}

void AmGetVersion(U08 *bMajor, U08 *bMinor) {
	*bMajor = VER_MAJOR;
	*bMinor = VER_MINOR;
	return;
}

U08 AmGetModuleInfo(U08 *pbMajorVer, U08 *pbMinorVer) {
	DbgOut(("  ================     AmGetModuleInfo()    =============\n"));
	return SimGetModuleInfo(pbMajorVer, pbMinorVer);
}

bool Get_Alcor_Disk(const char* bCurDisk) {
	//UCHAR 	bCurDisk;
	UINT uType;
	bool blStatus;

	snprintf(abCmdFile, sizeof abCmdFile - 1, "%s/%s\0", bCurDisk, CMD_FILE);

	if (access(abCmdFile, F_OK) == EXIT_SUCCESS) {
		int result = remove(abCmdFile);
		LOGI("==cmd file already  present deleting it====Result code= %d",
				result);
	}

	fprintf(stderr, "\nFile path : %s", abCmdFile);
	blStatus = writeBytes(abCmdFile, dummyPattern, sizeof dummyPattern);
	AmGetModuleInfo(0, 0);
	if (blStatus == TRUE)
		return TRUE;
	abCmdFile[0] = 0x00;
	return FALSE;	//FALSE
}

U08 SendSimCmd(U08 x, U08 *pbData, U16 y, U16 z) {
	SIM_CMD_BLOCK SimBlock;
	BOOL blStatus;	//BOOL

	memset((U08*) &SimBlock, 0, 512);
	memcpy(SimBlock.abHwSig, abHwSig, 0x10);
	memcpy(SimBlock.abFwSig, abCmdFwSig, 0x10);
	if (y) {
		memcpy(SimBlock.abData, pbData, y);
	}

	SimBlock.abDatagroup[0] = x;
	SimBlock.abDatagroup[3] = g_bA;
	SimBlock.abDatagroup[1] = (U08) y;
	SimBlock.abDatagroup[2] = (U08) (y >> 8);
	SimBlock.abDatagroup[4] = (U08) z;
	SimBlock.abDatagroup[5] = (U08) (z >> 8);
	SimBlock.abDatagroup[6] = 200;
	SimBlock.abDatagroup[7] = 0xFF;

	SimBlock.abFwSig[0x10] = 1;
	SimBlock.abFwSig[0x11] = 0x30;
	SimBlock.abFwSig[0x12] = 0x00;

	if (SimBlock.abDatagroup[0] == GET_SIM_MODULE_INFO) {
		U32 dTime;
		//SYSTEMTIME  SystemTime;

		//GetSystemTime(&SystemTime);
		//dTime = SystemTime.wMilliseconds + (SystemTime.wSecond*1000);
		//dTime += 65536 * (SystemTime.wHour * 60 + SystemTime.wMinute );
		dTime = 1000; //dummy number...
		*((U32 *) abRandNum) = dTime;
	}
	memcpy(&SimBlock.abDatagroup[8], abRandNum, 4);

	g_bB = SimBlock.abDatagroup[0];

	//blStatus = SdWriteFile((U08*)&SimBlock, 512);
	blStatus = writeBytes(abCmdFile, (U08*) &SimBlock, 512);

	if (blStatus == FALSE)
		return E_READ_FILE_ERR;
	else
		return E_SUCCESS;
}

U08 RcvSimRsp(U08 *pData, U16 *y) {
	U08 abData[512];
	U16 wLen;
	U16 wLength;
	U08 bResult;
	SIM_RSP_BLOCK *RspBlock;
	U08 blFirstTime;
	BOOL blStatus; //BOOL

	blFirstTime = 1;
	bResult = E_SUCCESS;
	while (1) {
		wLen = 512;

		//blStatus = 	SdReadFile(abData, &wLen, blFirstTime);
		blStatus = readBytes(abCmdFile, abData, &wLen);

		if (blStatus == FALSE) {

			g_bB = 0xFF;
			return E_WRITE_FILE_ERR; //FALSE
		}
		blFirstTime = 0;
		RspBlock = (SIM_RSP_BLOCK *) abData;

		if (memcmp(RspBlock->abHwSig, abRspSig, 0x10)) {

			bResult = E_HW_SIG_ERR;
			goto _err_exit;
		}

		if (memcmp(RspBlock->abFwSig, abRspFwSig, 0x10)) {

			bResult = E_FW_SIG_ERR;
			goto _err_exit;
		}

		if (RspBlock->abDatagroup[0] != g_bB) {

			bResult = E_CMD_MISMATCH_ERR;
			goto _err_exit;
		}

		if (RspBlock->abDatagroup[3] != g_bA) {

			bResult = E_SEQ_ERR;
			goto _err_exit;
		}

		if (RspBlock->abDatagroup[4] == E_SUCCESS) {
			wLength = 0;
			wLength |= RspBlock->abDatagroup[2];
			wLength <<= 8;
			//wLength |= RspBlock->abDatagroup[1];
			//2.4.2.R2 support : As per SD2SIM.java, reading second length byte
			wLength |= RspBlock->abDatagroup[1] & 0x00FF;
			if (*y < wLength) {

				goto _err_exit;
			}
			if (*y) {

				*y = wLength;

				memcpy(pData, RspBlock->abData, *y);
			}

			g_bA++;
			return bResult;
		} else if (RspBlock->abDatagroup[4] == E_TIME_EXTENSION
				|| RspBlock->abDatagroup[4] == 0x51) {//2.4.2.R2 support : Added RspBlockobj.abDatagroup[4] == 0x51 to support long response time

			continue;
		} else {
			bResult = RspBlock->abDatagroup[4];
			goto _err_exit;
		}
	}

	_err_exit: g_bB = 0xFF;
	return bResult;

}

/*
 BOOL SdWriteFile(U08 *pbData, U16 wLen)
 {
 HANDLE 	hFile;
 BOOL 	fSuccess;
 U16		wReturnedLen;

 if( wLen > 512 ){

 return FALSE;
 }
 if( abCmdFile[0] == 0x00 ) {

 return FALSE;
 }


 hFile = CreateFile(	abCmdFile, // file name
 GENERIC_READ | GENERIC_WRITE, // open r-w
 0,                    // do not share
 NULL,                 // default security
 CREATE_ALWAYS,        // overwrite existing
 FILE_FLAG_WRITE_THROUGH | FILE_FLAG_NO_BUFFERING |FILE_ATTRIBUTE_NORMAL,// normal file
 NULL);                // no template
 if (hFile == INVALID_HANDLE_VALUE) {

 return FALSE;//FALSE
 }

 wReturnedLen = 0;
 fSuccess = WriteFile(hFile,
 pbData,
 wLen,
 (U32*)&wReturnedLen,
 NULL);
 
 if (!fSuccess) {
 CloseHandle (hFile);

 return FALSE;
 }
 FlushFileBuffers(hFile);

 // Close the handles to the files.
 fSuccess = CloseHandle (hFile);

 if (!fSuccess) {
 
 return FALSE;
 }

 return TRUE;
 }


 BOOL SdReadFile(U08 *pbData, U16 *pwLen, U08 blFirstTime)
 {
 HANDLE 	hFile;
 BOOL 	fSuccess;

 if( *pwLen > 512 ){

 return FALSE;
 }
 if( abCmdFile[0] == 0x00 ) {

 return FALSE;
 }


 if( blFirstTime ) {
 hFile = CreateFile(	abCmdFile, // file name
 GENERIC_READ, // open r-w
 0,                    // do not share
 NULL,                 // default security
 OPEN_EXISTING,        // overwrite existing
 FILE_ATTRIBUTE_NORMAL,// normal file
 NULL);                // no template
 }
 else {
 hFile = CreateFile(	abCmdFile, // file name
 GENERIC_READ, // open r-w
 0,                    // do not share
 NULL,                 // default security
 OPEN_EXISTING,        // overwrite existing
 FILE_FLAG_NO_BUFFERING |FILE_ATTRIBUTE_NORMAL,// normal file
 NULL);                // no template
 }
 if (hFile == INVALID_HANDLE_VALUE) {

 return FALSE;//FALSE
 }

 fSuccess = ReadFile( hFile,
 pbData,
 *pwLen,
 (U32 *)pwLen,
 NULL);


 if (!fSuccess) {
 CloseHandle (hFile);

 return FALSE;
 }

 // Close the handles to the files.
 fSuccess = CloseHandle (hFile);

 if (!fSuccess) {

 return FALSE;
 }
 return fSuccess;
 }
 */
U08 SimGetModuleInfo(U08 *pbMajorVer, U08 *pbMinorVer) {
	U08 abData[0x10];
	U08 bStatus;
	U16 wLen;

	bStatus = SendSimCmd(GET_SIM_MODULE_INFO, NULL, 0, 0x10);
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}

	wLen = 0x10;
	bStatus = RcvSimRsp(abData, &wLen);
	if (bStatus != E_SUCCESS) {
		DbgOut(("GET_SIM_MODULE_INFO RcvRimRsp Error "));
		return bStatus;
	}
	if (wLen != 0x10) {

		return E_UNKNOWN_ERR;
	}DbgOut(("abData[0] = %02X",abData[0]));DbgOut(("abData[1] = %02X",abData[1]));

	if (pbMajorVer)
		*pbMajorVer = abData[0];
	if (pbMinorVer)
		*pbMinorVer = abData[1];
	return bStatus;
}

U08 SimPowerOn(U08 bmVoltage, U08 bResetType, U16 wmProtocol) {
	A a;
	U08 bStatus;
	U16 wLen = 34;
	HANDLE hFile;
	U08 abPar[22];
	/*
	 U08 abJCOP221[] = { 0x3B, 0xE9, 0x00, 0x00, 0x81, 0x31, 0xFE, 0x45, 0x4A,
	 0x43, 0x4F, 0x50, 0x34, 0x31, 0x56, 0x32, 0x32, 0xA7 }; //length: 18
	 U08 abJCOP241[] = { 0x3B, 0xF8, 0x13, 0x00, 0x00, 0x81, 0x31, 0xFE, 0x45,
	 0x4A, 0x43, 0x4F, 0x50, 0x76, 0x32, 0x34, 0x31, 0xB7 }; //length: 18
	 //0x00 ,0x18
	 /*U08 abParValue[] = {0x01, 0x00, 0x18, 0x00, 0x00, 0xED, 0x04, 0x56, 0x00, 0xA1,
	 0x00, 0xF0, 0x03, 0x0E,0x04, 0x10, 0xA4, 0x69, 0x00, 0x1F};

	 U08 abParValue[] = { 0x01, 0x01, 0x13, 0x01, 0x00, 0x64, 0x00, 0x01, 0x00,
	 0xA9, 0x02, 0xD1, 0x03, 0x2D, 0x04, 0x10, 0xA4, 0x69, 0x00, 0x5D,
	 0x00, 0xFE };

	 U08 abParValue241[] = { 0x01, 0x01, 0x13, 0x01, 0x00, 0x64, 0x00, 0x01,
	 0x00, 0xA9, 0x02, 0xD1, 0x03, 0x2D, 0x04, 0x10, 0xA4, 0x69, 0x00,
	 0x5D };
	 U08 abParValue241R3[] = { 0x01, 0x01, 0x13, 0x01, 0x00, 0x64, 0x00, 0x01,
	 0x00, 0xA9, 0x02, 0xD1, 0x03, 0x2D, 0x04, 0x10, 0xA4, 0x69, 0x00,
	 0x5D };

	 U08 abParValue221[] = { 0x00, 0x01, 0x11, 0x01, 0x00, 0x64, 0x00, 0x01,
	 0x00, 0xAB, 0x00, 0x46, 0x03, 0xB9, 0x04, 0x10, 0xA4, 0x69, 0x00,
	 0x74 };

	 U08 abParValue242R2[] = { 0x01, 0x01, 0x18, 0x01, 0x00, 0x64, 0x00, 0x01,
	 0x00, 0xF9, 0x07, 0xF0, 0x03, 0x0E, 0x04, 0x10, 0xA4, 0x69, 0x00,
	 0x1F };

	 ///{0x00, 0x01, 0x11, 0x01, 0x00, 0x64, 0x00, 0x01, 0x00, 0xAB, 0x00, 0x46, 0x03, 0xB9, 0x04, 0x10, 0xA4, 0x69, 0x00, 0x74};

	 // Infineon: 0x01, 0x00, 0x18, 0x00, 0x00, 0xED, 0x04, 0x56, 0x00, 0xA1,0x00, 0xF0, 0x03, 0x0E,0x04, 0x10, 0xA4, 0x69, 0x00, 0x1F};
	 //
	 //{0x01, 0x01, 0x13,0x01, 0x00, 0x64,0x00, 0x01,0x00, (U08)0xA9, 0x02, 0xD1, 0x03, 0x2D, 0x04, 0x10, (U08)0xA4, 0x69, 0x00, 0x5D, 0x00,0x00};

	 U08 abParValue_new[] = { 0x01, 0x01, 0x13, 0x01, 0x00, 0x64, 0x00, 0x01,
	 0x00, 0xA9, 0x02, 0xD1, 0x03, 0x2D, 0x04, 0x10, 0xA4, 0x69, 0x00,
	 0x5D };*/
//
	// As per kishan's feedback the below value of abpar should be used in java as well as c
	U08 abParValue[] = { 0x01, 0x01, 0x13, 0x01, 0x00, 0x64, 0x00, 0x01, 0x00,
			0xA9, 0x02, 0xD1, 0x03, 0x2D, 0x04, 0x10, 0xA4, 0x69, 0x00, 0x5D,
			0x00, 0xFE };
	U32 dwLen;
	U08 abData[4];
	int result = 0;

	memset(abData, 0, 4);
	abData[0] = 0xFF;
	abData[1] = 0x10;

	a.abData[0] = 0xE9;
	a.abData[1] = bmVoltage;
	a.abData[2] = 0;
	memset(abPar, 0, 22);

	bStatus = SendSimCmd(POWER_ON_CMD, (U08 *) &a, sizeof(a), 0xFFFF);
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}

	memset(abATR, 0, 64);
	bStatus = RcvSimRsp(abATR, &wLen);
	atrLen = wLen;		//34;
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}

	/*if(wLen == 18){
	 result = memcmp(abATR,abJCOP221,18);
	 if(result == 0){
	 memcpy(abPar,abParValue221,20);
	 }else{
	 result = memcmp(abATR, abJCOP241, 18);
	 if(result == 0){
	 memcpy(abPar,abParValue221,20);
	 } else {
	 memcpy(abPar, abParValue, 20);
	 }
	 }
	 } else {*/
	memcpy(abPar, abParValue, 22);
	//}

	//JCOP221_non_Preperso: 3BE900008131FE454A434F503431563232A7
	//JCOP241_non_preperso: 3BF81300008131FE454A434F5076323431B7

	/*	hFile = CreateFile(	"AlcorReader.par",
	 GENERIC_READ | GENERIC_WRITE, // open r-w
	 0,                    // do not share
	 NULL,                 // default security
	 OPEN_EXISTING,        // overwrite existing
	 FILE_ATTRIBUTE_NORMAL,// normal file
	 NULL);
	 if( hFile == INVALID_HANDLE_VALUE ){
	 DbgOut(("Open File Error!\n"));
	 return E_GET_HANDLE_ERR;
	 }
	 wLen = 22;
	 bStatus = ReadFile(hFile,
	 abPar,
	 wLen,
	 &dwLen,
	 NULL);
	 if( !bStatus ){
	 CloseHandle(hFile);
	 DbgOut(("Read File Error\n"));
	 return E_GET_HANDLE_ERR;
	 }
	 CloseHandle(hFile);
	 for( U32 i=0;i<dwLen;i++ ){
	 DbgOut(("%02X ",abPar[i]));
	 }
	 */

	if (abPar[0] == 0x01) {
		abData[1] |= abPar[1];
		abData[2] = abPar[2];
		abData[3] = CalLRC(abData, 3);
		bStatus = SimConfirm(abData, 4);
		if (bStatus != E_SUCCESS) {
			DbgOut(("\nSimConfirm Error\n"));
			return bStatus;
		}
	}

	bStatus = SimSetParam(&abPar[3], 19);

	if (bStatus != E_SUCCESS) {
		DbgOut(("\nSimSetParam Error!\n"));
		return bStatus;
	}

	return E_SUCCESS;

}
U08 AmGetATR(U08 *pbApduRsp, U16 wApduRspLen, U16 *pwApduRspReplyLen) {
	if (atrLen != 0) {
		memcpy(pbApduRsp, abATR, atrLen);
		*pwApduRspReplyLen = atrLen;
		return E_SUCCESS;
	}

	return (U08) E_FAIL;

}
U08 SimPowerOff() {
	U08 bStatus;
	U16 wRcvLen;
	bStatus = SendSimCmd(POWER_OFF_CMD, NULL, 0, 0);
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}

	wRcvLen = 0;
	bStatus = RcvSimRsp(NULL, &wRcvLen);
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}

	return bStatus;
}

U08 SimApduExchange(U08 *pbApduCmd, U16 wApduCmdLen, U08 *pbApduRsp,
U16 wApduRspLen, U16 *pwApduRspReplyLen) {
	U08 bStatus;

	bStatus = SendSimCmd(APDU_LEVEL_EXCH, pbApduCmd, wApduCmdLen, wApduRspLen);
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}

	*pwApduRspReplyLen = wApduRspLen;
	bStatus = RcvSimRsp(pbApduRsp, pwApduRspReplyLen);
	if (bStatus != E_SUCCESS) {

		return bStatus;
	}
	return bStatus;
}

U08 SimCharWrite(U08 *pbData, U16 wLen) {
	U08 bStatus;
	U16 wRcvLen;
	U08 bRetryCnt;
	if (wLen == 0)
		return E_SUCCESS;

	for (bRetryCnt = 0; bRetryCnt < 8; bRetryCnt++) {

		bStatus = SendSimCmd(CHAR_LEVEL_WRITE, pbData, wLen, 0);
		if (bStatus != E_SUCCESS) {

			continue;
			//return bStatus;
		}
		wRcvLen = 0;
		bStatus = RcvSimRsp(NULL, &wRcvLen);
		if (bStatus != E_SUCCESS) {

			if (bStatus == E_HW_SIG_ERR) {
				continue;
			}
			return bStatus;
		}
		break;
	}

	return bStatus;
}

U08 SimCharRead(U08 *pbData, U16 wReadLen) {
	U08 bStatus;
	U16 wLen = wReadLen;
	U08 bRetryCnt;

	if (wReadLen == 0)
		return E_SUCCESS;

	for (bRetryCnt = 0; bRetryCnt < 8; bRetryCnt++) {

		bStatus = SendSimCmd(CHAR_LEVEL_READ, NULL, 0, wReadLen);
		if (bStatus != E_SUCCESS) {

			continue;
			//return bStatus;
		}

		bStatus = RcvSimRsp(pbData, &wLen);
		if (bStatus != E_SUCCESS) {

			if (bStatus == E_HW_SIG_ERR) {
				continue;
			}
			return bStatus;
		}
		break;
	}

	if (wReadLen != wLen) {

		return E_UNKNOWN_ERR;
	}

	return bStatus;
}

U08 SimConfirm( U08 *pbData, U08 y) {
	U08 bStatus;
	U08 abData[4];
	memset(abData, 0, 4);
	bStatus = SimCharWrite(pbData, y);
	if (bStatus != E_SUCCESS) {
		DbgOut(("SimConfirm failed\n"));
		return bStatus;
	}

	bStatus = SimCharRead(abData, 4);
	if (bStatus != E_SUCCESS) {
		DbgOut(("SimConfirm failed\n"));
		return bStatus;
	}

	if (memcmp(abData, pbData, 4) != 0) {
		return E_UNKNOWN_ERR;
	}

	return bStatus;

}
U08 SimSetParam(U08 *pbData, U16 wLen) {
	U08 bStatus;

	bStatus = SendSimCmd(SET_PARAM, pbData, wLen, 0);
	if (bStatus != E_SUCCESS) {
		DbgOut(("SendSimCmd(SimSetParam) failed\n"));
		return bStatus;
	}

	wLen = 0;
	bStatus = RcvSimRsp(NULL, &wLen);
	if (bStatus != E_SUCCESS) {
		DbgOut(("RcvSimRsp(SimSetParam) failed\n"));
		return bStatus;
	}

	return bStatus;

}

U08 CalLRC(U08 *pbData, U16 wLen) {
	U16 x;
	U08 bLrc;

	bLrc = 0;
	for (x = 0; x < wLen; x++) {
		bLrc ^= pbData[x];
	}
	return bLrc;
}

U08 SPIWriteCmd(U08 *pb, U16 wApduCmdLen, U08 *pbData, U16 *wLen) {
	U08 bStatus = 0x00;
	U08 a = 0x01;
	U08 pb_updated[257];
	memcpy(pb_updated, pb, wApduCmdLen);
	pb_updated[wApduCmdLen] = (U08) 0x01;
	pb_updated[wApduCmdLen + 1] = (U08) 0xFF;
	if (*wLen < 2)
		return E_UNKNOWN_ERR;

	bStatus = SendSimCmd(EXTRA_SC_FUNCTION, pb_updated, (wApduCmdLen + 2),
			0x10);
	if (bStatus != E_SUCCESS) {
		return bStatus;
	}
	bStatus = RcvSimRsp(pbData, wLen);
	return bStatus;
}

// TODO: Chain this to amRF Stephen_Code_Review
// TODO: No need to pass this buffer, remove args. Stephen_Code_Review
//2.4.2.R2 support : As per SD2SIM.java *pbData is 272, here we send 264
// As per kishan's feedback size of pbdata will be 432 which is defined as MAX_RESPONSE_SIZE in cardcontrol.h
U08 AmRFOn(U08 *pbData, U16 *wLen) {
	U08 bStatus = 0x00;
	U08 a = 0x00;
	if (*wLen < 2)
		return E_UNKNOWN_ERR;
	bStatus = SendSimCmd(EXTRA_SC_FUNCTION, &a, 1, 0x10);
	if (bStatus != E_SUCCESS) {
		return bStatus;
	}
	bStatus = RcvSimRsp(pbData, wLen);
	return bStatus;
}

// TODO: Chain this to amRF Stephen_Code_Review
// TODO: No need to pass this buffer, remove args. Stephen_Code_Review
//2.4.2.R2 support : As per SD2SIM.java *pbData is 272, here we send 264
// As per kishan's feedback size of pbdata will be 432 which is defined as MAX_RESPONSE_SIZE in cardcontrol.h
U08 AmRFOff(U08 *pbData, U16 *wLen) {
	U08 bStatus = 0x00;
	U08 a = 0x01;
	if (*wLen < 2)
		return E_UNKNOWN_ERR;
	bStatus = SendSimCmd(EXTRA_SC_FUNCTION, &a, 1, 0x10);
	if (bStatus != E_SUCCESS) {
		return bStatus;
	}
	bStatus = RcvSimRsp(pbData, wLen);
	return bStatus;
}

U08 AmRF(U08 ControlCode, U08 *pbData, U16 *wLen) {
	U08 bStatus = 0x00;
	U08 a = ControlCode;
	if (*wLen < 2)
		return E_UNKNOWN_ERR;
	bStatus = SendSimCmd(EXTRA_SC_FUNCTION, &a, 1, 0x10);
	if (bStatus != E_SUCCESS) {
		return bStatus;
	}
	bStatus = RcvSimRsp(pbData, wLen);

	// TODO: Check buffer contents for success code.  Return failure if not 9000. Stephen_Code_Review

	return bStatus;
}

/*U08 *pbData,U16 *wLen*/
U08 AmSCEnable() {
	U08 bStatus = 0x00;
	U08 a = 0x02;
	//2.4.2.R2 support : As per SD2SIM.java
	// As per kishan's feedback size of pbdata will be 432 which is defined as MAX_RESPONSE_SIZE in cardcontrol.h
	//U08 pbData[0x10]; In java this is pbData[256]
	U08 pbData[MAX_RESPONSE_SIZE];
	U16 wLen = MAX_RESPONSE_SIZE;
	if (wLen < 2)
		return E_UNKNOWN_ERR;
	bStatus = SendSimCmd(EXTRA_SC_FUNCTION, &a, 1, 0x10);
	if (bStatus != E_SUCCESS) {
		return bStatus;
	}
	bStatus = RcvSimRsp(pbData, &wLen);
	return bStatus;
}

/*
 * returns 1 if present, 0 if absent
 */
int isCardPresent() {
	if (!doesPollingFileExist()) {
		return (createPollingFile() == EXIT_SUCCESS);
	} else {
		return (readPollingFile() == EXIT_SUCCESS);
	}
}

int doesPollingFileExist() {
	return access(pollFile, F_OK) == EXIT_SUCCESS;
}

int createPollingFile() {
	snprintf(pollFile, sizeof pollFile - 1, "%s/%s\0", tyfone_csc_path,
	POLL_FILE);

	if (doesPollingFileExist) {
		LOGI("=============Polling file already exists============== \n");
		LOGI("=============Removing file =%d \n", remove(pollFile));
	}
	LOGI("=============Creating polling file============== \n");
	return writeBytes(pollFile, pollBytes, sizeof pollBytes);
}

int readPollingFile() {
	//LOGI("=============reading polling file============== \n");
	static char* pollBuffer[SECTOR_SIZE];
	return readBytes(pollFile, pollBuffer, sizeof pollBuffer);
}
int removefiles() {
	int resultCmdFile, resultPollFile;

	if (access(abCmdFile, F_OK) == EXIT_SUCCESS) {
		resultCmdFile = remove(abCmdFile);
		//	LOGI("=======deleted cmd file======Result code= %d\n", resultCmdFile);
	}
	abCmdFile[0] = 0x00;

	if (doesPollingFileExist()) {
		resultPollFile = remove(pollFile);
		//	LOGI("======deleted polling file===Result code= %d\n", resultPollFile);
	}
	pollFile[0] = 0x00;

	return (resultCmdFile && resultPollFile);
}
int getCardStatus() {
	int status = writeBytes(abCmdFile, dummyPattern, sizeof dummyPattern);
	return status;
}
