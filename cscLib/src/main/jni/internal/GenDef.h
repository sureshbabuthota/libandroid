/* 
* © 2004-2014, Tyfone, Inc. All rights reserved.
* Patented and other Patents pending.  
* All trademarks are property of their respective owners. 
* SideTap, SideSafe, SideKey, The Connected Smart Card,  
* CSC are trademarks of Tyfone, Inc. 
* For questions visit: www.tyfone.com 
*/ 

#ifndef GENDEF_H
#define GENDEF_H

#pragma pack(1)


// Changed the max buffer size as per kishan's feedback.
//#define MAX_BUFFER_SIZE			264
#define MAX_BUFFER_SIZE			432

#define CMD_METHOD_VIA_FILE		0x01
#define REPEAT_SAME_RSP			0x02

#define			IDATA_TYPE		0x00
#define 		XDATA_TYPE		0x01
#define 		CODE_TYPE		0x02

#define		POWER_ON_CMD		0x00
#define		POWER_OFF_CMD		0x01
#define		CHAR_LEVEL_WRITE	0x02
#define		CHAR_LEVEL_READ		0x03
#define 	SET_PARAM			0x04
#define 	GET_VAL_CMD			0x05
#define 	SET_VAL_CMD			0x06
#define 	GET_SIM_MODULE_INFO	0x07
#define 	APDU_LEVEL_EXCH		0x08
#define		TPDU_LEVEL_T1_EXCH	0x09
#define 	EXTRA_SC_FUNCTION	0x0A
#define		TPDU_LEVEL_T0_EXCH	0x0B

#define E_SUCCESS				0x00
#define E_TIME_EXTENSION		0x01
#define E_OVERCURRENT_ERR		0x02
#define E_ICC_MUTE_ERR			0x03
#define E_PARITY_ERR			0x04
#define E_OVER_UNDER_RUN_ERR	0x05
#define E_ATR_BAD_TS			0x06
#define E_ATR_INVALID_VALUE		0x07
#define E_ATR_BAD_LRC			0x08
#define E_ATR_BAD_FOR_EMV		0x09
#define E_SCARD_STATUS_TOO_LOW	0x0A
#define E_UNKNOWN_PROTOCOL_TYPE	0x0B
#define E_UNKNOWN_ERR			0x0C
#define E_PPS_PCK_ERR			0x0D
#define E_PPS_PPSS_ERR			0x0E
#define E_PPS_PPS0_ERR			0x0F
#define E_PPS_UNKNOWN_ERR		0x10
#define E_INVALID_PROTOCOL_ERR	0x11
#define E_PPS_PPS1_ABSENT		0x12

#define E_NO_MEMORY_ERR			0x13
#define E_HW_ERR				0x14

#define E_CARD_ABSENT			0x15
#define E_CARD_CHANGED			0x16
#define E_HOST_TIMER_UP			0x17

#define E_WRITE_FILE_ERR		0x20
#define E_HW_SIG_ERR			0x21
#define E_FW_SIG_ERR			0x22
#define E_CMD_MISMATCH_ERR		0x23
#define E_SEQ_ERR				0x24
#define E_READ_FILE_ERR			0x25
#define E_APDU_FORMAT_ERR		0x26
#define E_T0_PROTOCOL_ERR		0x27

#define E_TPDU_COMPLETED_ERR	0x30
#define E_INVALID_T1_BLOCK		0x31
#define E_T1_EDC_PARITY_ERR		0x32
#define E_UNEXPECTED_T1_BLOCK	0x33
#define E_T1_ABORT_ERR			0x34


/* 0x40 ~ 0x4F are reserved for AP/Library */
#define E_NO_READER_ERR			0x40
#define E_GET_HANDLE_ERR		0x41
#define E_GET_INI_FILE_ERR		0x42
#define E_BUFFER_TOO_SHORT		0x43


/*The following is only for ATR sub */
#define E_ATR_REJECT			0x00
#define E_ICC_REJECT			0x80



#define SC_OFFSET				0x10

/* g_bmFeature */
#define OV_DETECTED_ENABLED		0x01
#define TEST_MODE				0x02
#define EMV_MODE				0x04
#define TIME_OUT_DETECTION		0x08
#define EXCH_LEVEL_MASK			0x30
#define CHAR_LEVEL				0x00
#define TPDU_LEVEL				0x10
#define APDU_LEVEL				0x20
#define ENHANCE_HOST_STABILITY	0x40
#define CHECK_CARD_EXIST		0x80

/* bmAltFeature */


#define  DEFAULT_F_D			0x11

#define  SCARD_PROTOCOL_T0 			0x0001
#define  SCARD_PROTOCOL_T1 			0x0002
//#define  SCARD_PROTOCOL_DEFAULT 	0x0000
#define  SCARD_PROTOCOL_OPTIMAL 	0x0004

/* The following is the time out counter */
#define ATR_1ST_CHAR_CLK_CNT		(42000)
#define ATR_2CHAR_96ETU_CNT			(10080/96)


/* This is already defined in pcscdefines as 33 */
/*
#define MAX_ATR_SIZE	34
*/


#define SC_POWER_UNCHANGED		0x00
#define SC_1_8_VOLTAGE			0x01
#define SC_3_VOLTAGE			0x02
#define SC_VOLTAGE_NUM			0x02


#define SCARD_UNKNOWN     0   
#define SCARD_ABSENT      1  
#define SCARD_PRESENT     2   
#define SCARD_SWALLOWED   3  
#define SCARD_POWERED     4  
#define SCARD_NEGOTIABLE  5  
#define SCARD_SPECIFIC    6   

              
typedef struct A {

	U08			abData[22];
 
}A, *pA;


typedef struct _SIM_CMD_BLOCK {
	U08 abHwSig[0x10];
	U08 abFwSig[0x30];
	U08 abDatagroup[16];
	U08 abData[0x200-0x50];	
}SIM_CMD_BLOCK, *PSIM_CMD_BLOCK;


typedef struct _SIM_RSP_BLOCK {
	U08 abHwSig[0x10];
	U08 abFwSig[0x30];
	U08 abDatagroup[16];
	U08 abData[0x200-0x50];	
}SIM_RSP_BLOCK, *PSIM_RSP_BLOCK;



U08 SimCharNullRead(U16 wLen);
U08 SimCharRead(U08 *pbData, U16 y);
U08 SimCharWrite(U08 *pbData, U16 y);
U08 SimSetParam(U08 *pbData,U16 wLen);
U08 SimConfirm(U08 *pbData,U08 y);
U08 SimPowerOff();
U08 SimPowerOn(U08 x, U08 y, U16 z);
U08 SimGetModuleInfo(U08 *pbMajorVer, U08 *pbMinorVer);
U08 SimApduExchange(U08 *pbApduCmd, U16 wApduCmdLen, U08 *pbApduRsp, U16 wApduRspLen, U16 *pwApduRspReplyLen);
#endif
