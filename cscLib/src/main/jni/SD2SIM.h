/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.
 * Patented and other Patents pending.
 * All trademarks are property of their respective owners.
 * SideTap, SideSafe, SideKey, The Connected Smart Card,
 * CSC are trademarks of Tyfone, Inc.
 * For questions visit: www.tyfone.com
 */

// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the SD2SIM_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// SD2SIM_API functions as being imported from a DLL, wheras this DLL sees symbols
// defined with this macro as being exported.
//#ifdef SD2SIM_EXPORTS
//#define SD2SIM_API __declspec(dllexport)
//#else
//#define SD2SIM_API __declspec(dllimport)
//#endif
#ifndef SD2SIM_ALCOR_MICRO_PBOC
#define SD2SIM_ALCOR_MICRO_PBOC 

#ifdef __cplusplus
extern "C" {
#endif 

#define VER_MAJOR		0x01
#define VER_MINOR		0x01

#define U08				unsigned char
#define U16				unsigned short
#define U32				unsigned long

#define S08				char
#define S16				unsigned short
#define UINT				unsigned int

#define BOOL				bool
#define HANDLE				long
#define TRUE			0x00
#define FALSE			0x01

#define E_SUCCESS		0x00
#define CMD_FILE		"TYFONE.SIM"
#define POLL_FILE		"POLLING.SIM"
#define MAX_ATR_LEN		34
#define MAX_APDU_LEN	258*2
#define MAX_RESP_LEN	258*2

#define MAX_PATH_SIZE 256

#define CREATE_NEW          1
#define CREATE_ALWAYS       2
#define OPEN_EXISTING       3
#define OPEN_ALWAYS         4
#define TRUNCATE_EXISTING   5

#define GENERIC_READ                     (0x80000000L)
#define GENERIC_WRITE                    (0x40000000L)
#define GENERIC_EXECUTE                  (0x20000000L)
#define GENERIC_ALL                      (0x10000000L)

#define FILE_FLAG_WRITE_THROUGH         0x80000000
#define FILE_FLAG_OVERLAPPED            0x40000000
#define FILE_FLAG_NO_BUFFERING          0x20000000
#define FILE_FLAG_RANDOM_ACCESS         0x10000000
#define FILE_FLAG_SEQUENTIAL_SCAN       0x08000000
#define FILE_FLAG_DELETE_ON_CLOSE       0x04000000
#define FILE_FLAG_BACKUP_SEMANTICS      0x02000000
#define FILE_FLAG_POSIX_SEMANTICS       0x01000000
#define FILE_FLAG_OPEN_REPARSE_POINT    0x00200000
#define FILE_FLAG_OPEN_NO_RECALL        0x00100000
#define FILE_FLAG_FIRST_PIPE_INSTANCE   0x00080000

#define FILE_SHARE_READ                 0x00000001  
#define FILE_SHARE_WRITE                0x00000002  
#define FILE_SHARE_DELETE               0x00000004  
#define FILE_ATTRIBUTE_READONLY             0x00000001  
#define FILE_ATTRIBUTE_HIDDEN               0x00000002  
#define FILE_ATTRIBUTE_SYSTEM               0x00000004  
#define FILE_ATTRIBUTE_DIRECTORY            0x00000010  
#define FILE_ATTRIBUTE_ARCHIVE              0x00000020  
#define FILE_ATTRIBUTE_DEVICE               0x00000040  
#define FILE_ATTRIBUTE_NORMAL               0x00000080  
#define FILE_ATTRIBUTE_TEMPORARY            0x00000100  
#define FILE_ATTRIBUTE_SPARSE_FILE          0x00000200  
#define FILE_ATTRIBUTE_REPARSE_POINT        0x00000400  
#define FILE_ATTRIBUTE_COMPRESSED           0x00000800  
#define FILE_ATTRIBUTE_OFFLINE              0x00001000  
#define FILE_ATTRIBUTE_NOT_CONTENT_INDEXED  0x00002000  
#define FILE_ATTRIBUTE_ENCRYPTED            0x00004000  
#define FILE_ATTRIBUTE_VIRTUAL              0x00010000
//***********************************************************
#define INVERSE_CONVENTION						0x01
#define TA2_EXIST								0x02

//**********************************************************

#define TA_BIT			0x10
#define TB_BIT			0x20
#define TC_BIT			0x40
#define TD_BIT			0x80

#define	 COLD_RESET_TYPE		0x00
#define  WARM_RESET_TYPE		0x01
#define  FAILED_RESET_TYPE		0x02

#define UI_18V33V50V                0x07
#define UI_18V50V                   0x05
#define UI_33V50V                   0x03
#define UI_33V18V                   0x06
#define UI_18V                      0x04
#define	UI_33V						0x02	//	Class indicator, ISO 7816-3 table 11
#define	UI_50V						0x01	//	Class indicator, ISO 7816-3 table 11

#define _HRESULT_TYPEDEF_(_sc) ((HRESULT)_sc)
#define E_FAIL                 _HRESULT_TYPEDEF_(0x80004005L)

U08 AmIsCardPresent();
U08 AmConnect(const char* diskLetter);
U08 AmDisconnect();
U08 AmPowerOn(U16 wProtocol);
U08 AmPowerOff();
U08 AmTransmit(U08 *pbApduCmd, U16 wApduCmdLen, U08 *pbApduRsp, U16 wApduRspLen,
U16 *pwApduRspReplyLen);
U08 AmGetATR(U08 *pbApduRsp, U16 wApduRspLen, U16 *pwApduRspReplyLen);
void AmGetVersion(U08 *bMajor, U08 *bMinor);
U08 AmGetModuleInfo(U08 *pbMajorVer, U08 *pbMinorVer);
U08 AmRFOn();
U08 AmRFOff();
U08 AmSCEnable();
int getCardStatus();
int createPollingFile();
int readPollingFile();
int removefiles();
#ifdef __cplusplus
}
#endif
#endif
