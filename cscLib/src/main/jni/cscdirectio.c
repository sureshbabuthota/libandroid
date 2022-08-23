/*
 * © 2004-2014, Tyfone, Inc. All rights reserved.
 * Patented and other Patents pending. 
 * All trademarks are property of their respective owners.
 * SideTap, SideSafe, SideKey, The Connected Smart Card, 
 * CSC are trademarks of Tyfone, Inc.
 * For questions visit: www.tyfone.com
 */

#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

#include "SD2SIM.h"
#include "cscdirectio.h"
#include "internal/types.h"
#include "internal/GenDef.h"
#include "internal/CardControl.h"
#include <android/log.h>

#define TAG "cscdirectio"

#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)) 
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)) 
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO   , TAG, __VA_ARGS__)) 
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN   , TAG, __VA_ARGS__)) 
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR  , TAG, __VA_ARGS__))

/**/
#define READER_NAME "Tyfone Connected Smart Card 00 00"
/*CSCException class path used to throw exceptions*/
#define EXCEPTION_CLASS "com/tyfone/csc/exception/CSCException"
/*Error code used to throw exceptions from native*/
long ERROR_CODE_FOR_NATIVE_ERRORS = 0x1006;
/*Global variable storing Tyfone ConnectedSmartCard path set from Java layer*/
char * tyfone_csc_path;

/*boolean isInitialized indicating whether the card has been initialized or not.*/
bool isInitialized = false;

char* byteArrayToString(PUCHAR byteArray, int numBytes);
int sc_hex_to_bin(const char *in, char *out, size_t *outlen);

/**
 *Logs and throws exceptions for errors using printf format string
 */
int throwException(JNIEnv* env, const char *msg) {
	// TODO  Make constant MAX_EXCEPTION_MESSAGE_SIZE  Stephen_Code_Review
	char buffer[512];
	// TODO  Always use strncpy, not strcpy  Stephen_Code_Review
	strcpy(buffer, msg);
	LOGE("Exception in native -%s", buffer);
	return (*env)->ThrowNew(env, (*env)->FindClass(env, EXCEPTION_CLASS),
			buffer);
}
/*
 * Convenience method to factor connection code out of each of the functions
 */
int connectCard(JNIEnv* env) {

	LOGI("Establishing smart card context");

	if (AmIsCardPresent() == TRUE) {
		return EXIT_SUCCESS;
	} else {
		LOGI("Connecting to card at path: (%s)", tyfone_csc_path);
		int result = AmConnect(tyfone_csc_path);

		if (result == EXIT_SUCCESS) {
			//AmPowerOn(0);
			return EXIT_SUCCESS;
		} else {
			LOGE("Unable to connect to card");
			char errorMessage[512];
			sprintf(errorMessage, "Error in connecting to card. Error is- (%d)",
					result);
			throwException(env, errorMessage);
		}
	}
}

/*
 * Convenience method to factor cleanup code out of each of the functions
 */
int disconnectCard(JNIEnv* env) {

	LOGI("Disconnecting from smart card");
	return AmDisconnect();
}

/**This jni method Initilizes CSC by setting the path to Tyfone CSC.*/
jboolean JNICALL Java_com_tyfone_csc_communication_SDIOCommunication_initializeCSC(
		JNIEnv * env, jobject thiz, jbyteArray path) {
	if (isInitialized) {
		LOGV("cscdirectio - already initialized. Returning true. ");
		return true;
	}

	LOGV("cscdirectio - setting Path in native... ");
	// converting jbyteArray  to const char  *variable
	jbyte* const byte_array_carray = (*env)->GetByteArrayElements(env, path, 0);
	LOGV("cscdirectio - sdcard path set in native= %s", byte_array_carray);


	tyfone_csc_path = (const char*) byte_array_carray;

	LOGV("cscdirectio - sdcard path set in native= %s", tyfone_csc_path);

	isInitialized = true;

	LOGV("cscdirectio - set isInitialized to true");

	createPollingFile();

	return true;
}

/**This jni method finalizes CSC by disconnecting CSC and closing files.*/
void Java_com_tyfone_csc_communication_SDIOCommunication_finalizeCSC(JNIEnv* env) {
	LOGI("=============in directio finalize============== \n");
	if (isInitialized == false)
		throwException(env, "The CSC has not been initialized.");
	else {
		isInitialized = false;
		disconnectCard(env);
		removefiles();
		LOGV("cscdirectio - Disconnected from card.");
	}

}

/**This jni method writes the given apdu to the CSC  */
jstring Java_com_tyfone_csc_communication_SDIOCommunication_writeToCard(JNIEnv* env,
		jobject thiz, jstring apdu) {
	jstring stringToReturn = "";
	if (isInitialized == false) {
		throwException(env, "The CSC has not been initialized.");
		return stringToReturn;
	}
	unsigned char inputAPDU[MAX_BUFFER_SIZE];
	int inputAPDULen = sizeof(inputAPDU);

	unsigned char bSendBuffer[MAX_BUFFER_SIZE];
	unsigned char bRecvBuffer[MAX_BUFFER_SIZE];
	DWORD recv_length = sizeof(bRecvBuffer);

	// Get apdu string from java
	char *apduChars = (char*) (*env)->GetStringUTFChars(env, apdu, NULL);
	LOGD("cscdirectio: sendAPDU - apduChars: %s", apduChars);

	// Convert string to bytes
	int err = sc_hex_to_bin(apduChars, inputAPDU, &inputAPDULen);
	(*env)->ReleaseStringUTFChars(env, apdu, apduChars);
	if (err || (inputAPDULen == 0)) {
		char errorMessage[512];
		sprintf(errorMessage, "Error parsing apdu. Error is- (%d)", err);
		throwException(env, errorMessage);
		return NULL;
	}
	LOGD("sendAPDU - inputAPDULen: %i", inputAPDULen);

	connectCard(env);

	LOGI("Sending APDU");

	int result = AmTransmit(inputAPDU, inputAPDULen, bRecvBuffer, recv_length,
			&recv_length);

	if (result != E_SUCCESS) {
		char errorMessage[512];
		sprintf(errorMessage, "Error in sending APDU Error Code- %d.", result);
		throwException(env, errorMessage);
	}
	//char* respString = byteArrayToString(bRecvBuffer, recv_length);

	// bytes -> hex -> java string
	char *response = byteArrayToString(bRecvBuffer, recv_length);

	LOGV("Response from APDU: %s", response);

	stringToReturn = (*env)->NewStringUTF(env, response);

	free(response);

	return stringToReturn;
}
/**This jni method writes the given apdu to the CSC  */
jbyteArray Java_com_tyfone_csc_communication_SDIOCommunication_writeData(JNIEnv* env,
		jobject thiz, jbyteArray apdu) {
	jbyteArray responseArray;
	if (isInitialized == false) {
		throwException(env, "The CSC has not been initialized.");
		return responseArray;
	}

	unsigned char bSendBuffer[MAX_BUFFER_SIZE];
	unsigned char bRecvBuffer[MAX_BUFFER_SIZE];
	DWORD recv_length = sizeof(bRecvBuffer);

	     int inputAPDULen = (*env)->GetArrayLength(env,apdu);
     unsigned char* inputAPDU =  (unsigned char*) malloc(inputAPDULen);

    (*env)->GetByteArrayRegion (env,apdu, 0, inputAPDULen, (jbyte*)(inputAPDU));

/*
    char *apdustring = byteArrayToString(inputAPDU, inputAPDULen);
      		    LOGD("cscdirectio: sendAPDU - apdustring: %s", apdustring);


	LOGD("sendAPDU - inputAPDULen: %i", inputAPDULen);
*/

	connectCard(env);

	LOGI("Sending APDU");

	int result = AmTransmit(inputAPDU, inputAPDULen, bRecvBuffer, recv_length,
			&recv_length);

	if (result != E_SUCCESS) {
		char errorMessage[512];
		sprintf(errorMessage, "Error in sending APDU Error Code- %d.", result);
		throwException(env, errorMessage);
	}

	responseArray= (*env)->NewByteArray(env,(jsize)recv_length);
    unsigned char* byte_array_resparray = (*env)->GetByteArrayElements(env, responseArray, 0);
    memcpy(byte_array_resparray,bRecvBuffer,recv_length);
    LOGV("Response in bytes:", byte_array_resparray);

     (*env)->ReleaseByteArrayElements(env,responseArray, byte_array_resparray, 0);

	return responseArray;
}



/**This method powers ON or powers OFF the Secure element in CSC depending on the Control code.*/
void setPower(JNIEnv *env, DWORD controlCode) {

	if (controlCode != POWER_ON && controlCode != POWER_OFF) {
		LOGV("cscdirectio: in setPower invalid controlCode, returning");
		return;
	}

	U08 status;
	long err;

	connectCard(env);

	if (controlCode == POWER_ON) {
		LOGI("Turning Power ON");
		status = AmPowerOn(0);
		if (status != E_SUCCESS) {
			char errorMessage[512];
			sprintf(errorMessage, "Power ON was unsuccessful.Error Code- %d",
					status);
			throwException(env, errorMessage);
		}
	} else if (controlCode == POWER_OFF) {
		LOGI("Turning Power OFF");
		status = AmPowerOff();
		if (status != E_SUCCESS) {
			char errorMessage[512];
			sprintf(errorMessage, "Power OFF was unsuccessful.Error Code- %d",
					status);
			throwException(env, errorMessage);
		}
	}
	err = disconnectCard(env);
	if (err)
		return;
}

/**This jni method powers ON the secure element in CSC.*/
void Java_com_tyfone_csc_communication_SDIOCommunication_setPowerOn(JNIEnv* env) {
	if (isInitialized == false)
		throwException(env, "The CSC has not been initialized.");
	else {
		LOGV("cscdirectio: PowerOn");
		setPower(env, POWER_ON);
	}
}

/**This jni method powers OFF the secure element in CSC.*/
void Java_com_tyfone_csc_communication_SDIOCommunication_setPowerOff(JNIEnv* env) {

	if (isInitialized == false)
		throwException(env, "The CSC has not been initialized.");
	else {
		LOGV("cscdirectio: PowerOff");
		setPower(env, POWER_OFF);
	}
}

/**This method switches Rf ON or OFF in CSC depending on the Control code.*/
void setRF(JNIEnv *env, DWORD controlCode) {

	if (controlCode != RF_ON && controlCode != RF_OFF) {
		LOGV("cscdirectio: in setRF invalid controlCode, returning");
		return;
	}

	unsigned char *bSendBuffer = NULL;
	DWORD send_length = 0;
	unsigned char bRecvBuffer[MAX_BUFFER_SIZE];
	DWORD recv_length = sizeof(bRecvBuffer);
	DWORD numBytesReceived = sizeof(bRecvBuffer);
	long err;

	connectCard(env);
	// TODO: Remove switch, use single flow, put if/else around AmRFOn/AmRFOff.  Stephen_Code_Review

	switch (controlCode) {
	case RF_ON: {
		LOGI("Turning RF On");
		U16 bufLen = MAX_RESPONSE_SIZE;
		UCHAR *buffer = malloc(bufLen);
		U08 err = AmRFOn(buffer, &bufLen);
		if (err) // Fail
		{
			numBytesReceived = 0;
			free(buffer);
			throwException(env, "Unable to turn RF ON.");
			return;
		} else // Success
		{
			memcpy(bRecvBuffer, buffer, sizeof(buffer));
			recv_length = sizeof(buffer);
			numBytesReceived = sizeof(buffer);
			free(buffer);
			return;
		}
	}
		break;
	case RF_OFF: {
		LOGI("Turning RF OFF");
		U16 bufLen = MAX_RESPONSE_SIZE;
		UCHAR *buffer = malloc(bufLen);
		//return toggleRF(dwControlCode, opName, RxBuffer, RxLength, pdwBytesReturned);
		U08 err = AmRFOff(buffer, &bufLen);
		if (err) // Fail
		{
			numBytesReceived = 0;
			free(buffer);
			throwException(env, "Unable to turn RF OFF");
			return;
		} else // Success
		{
			memcpy(bRecvBuffer, buffer, sizeof(buffer));
			recv_length = sizeof(buffer);
			numBytesReceived = sizeof(buffer);
			free(buffer);
			return;
		}
	}
		break;

	default:
		// unknown control code
		fprintf(stderr, " Unknown ControlCode: %8X", controlCode);
		numBytesReceived = 0;
		return;

	}

	// TODO: This code is unreachable.  Move this logic to AmRF. Stephen_Code_Review

	err = disconnectCard(env);
	if (err)
		return;

	// Check the response
	char *response = byteArrayToString(bRecvBuffer, numBytesReceived);
	if ((numBytesReceived < 2) || (bRecvBuffer[0] != 0x90)
			|| (bRecvBuffer[1] != 0x00)) {
		char errorMessage[512];
		sprintf(errorMessage, "Response code indicating error: (%s)", response);
		throwException(env, errorMessage);
	}
	free(response);
}

void Java_com_tyfone_csc_communication_SDIOCommunication_setRFOn(JNIEnv* env) {
	if (isInitialized == 0)
		throwException(env, "The CSC has not been initialized.");
	else {
		LOGV("cscdirectio: RFOn");
		setRF(env, RF_ON);
	}
}

void Java_com_tyfone_csc_communication_SDIOCommunication_setRFOff(JNIEnv* env) {
	if (isInitialized == 0)
		throwException(env, "The CSC has not been initialized.");
	else {
		LOGV("cscdirectio: RFOff");
		setRF(env, RF_OFF);
	}
}
/**This jni method gives the firmware version in the CSC*/
void Java_com_tyfone_csc_communication_SDIOCommunication_getFirmwareVersion(JNIEnv* env) {
	LOGV("cscdirectio: getFirmwareVersion");

}
/**This jni method gets the atr(answer to reset) from the CSC*/
jstring Java_com_tyfone_csc_communication_SDIOCommunication_getATR(JNIEnv* env) {
	if (isInitialized == 0)
		throwException(env, "The CSC has not been initialized.");

	LOGV("cscdirectio - getATR");

	DWORD dwReaderLen, dwState, dwProt, dwAtrLen;
	// TODO: Cleanup unused variables.  Stephen_Code_Review

	unsigned char pbAtr[MAX_ATR_SIZE];
	size_t atrStringLen = MAX_ATR_SIZE * 3 + 1;
	long err;

	err = connectCard(env);
	if (err)
		return NULL;

	dwAtrLen = MAX_ATR_SIZE;

	UCHAR tempBuffer[512];
	// TODO: Change this to a constant.  MAX_RESPONSE_SIZE?  Stephen_Code_Review

	//PDWORD *AtrLength = 0;
	// TODO: check for success/failure
	//seretGetATR(TUP_HEADER, tempBuffer, sizeof tempBuffer);
	AmGetATR(tempBuffer, sizeof tempBuffer, &dwAtrLen);

	// copy Response to static global variable and to atr parameter
	//*AtrLength = MAX_ATR_SIZE;
	//copyResponseBytes(tempBuffer, Atr, AtrLength);
	memcpy(pbAtr, tempBuffer, dwAtrLen);

	err = disconnectCard(env);
	if (err)
		return NULL;

	// bytes -> hex -> java string
	char *atrString = byteArrayToString(pbAtr, dwAtrLen);
	LOGV("ATR: %02x, %02x", pbAtr[0], pbAtr[1]);
	LOGV("ATR: %s", atrString);

	jstring stringToReturn = (*env)->NewStringUTF(env, atrString);
	free(atrString);

	return stringToReturn;
}

/**This jni method reads from the csc.*/
jboolean JNICALL Java_com_tyfone_csc_communication_SDIOCommunication_readFromPollingFile(
		JNIEnv * env, jobject thiz) {
	if (isInitialized == 0)
		throwException(env, "The CSC has not been initialized.");
	if (readPollingFile() == E_SUCCESS) {
		return true;
	} else {
		return false;
	}
}

///////////////////////////// Utilities /////////////////////////////////
char nibbleToChar(UCHAR nibble) {
	return (char) (nibble > 9 ? nibble + 0x37 : nibble + 0x30);
}

UCHAR getUpperNibble(UCHAR byte) {
	return (UCHAR) (byte >> 4);
}

UCHAR getLowerNibble(UCHAR byte) {
	return (UCHAR) (byte & 0xF);
}
/*
 * This method converts a byte array to a string(character pointer)
 * Caller is responsible for freeing the returned string to avoid leaks
 */
char* byteArrayToString(PUCHAR byteArray, int numBytes) {

	// check for null pointer or no bytes
	if (numBytes == 0 || !byteArray) {
		return "";
	}

	// need 1 character represent each nibble (2 chars per byte), plus null at end
	const int numNibbles = 2 * numBytes;
	char* output = (char*) malloc(numNibbles + 1);

	UCHAR upperNibble;
	UCHAR lowerNibble;

	int byteCt;
	int nibbleCt;
	for (byteCt = 0, nibbleCt = 0; byteCt < numBytes; byteCt++) {

		UCHAR currentByte = byteArray[byteCt];
		upperNibble = getUpperNibble(currentByte);
		lowerNibble = getLowerNibble(currentByte);

		output[nibbleCt++] = nibbleToChar(upperNibble);
		output[nibbleCt++] = nibbleToChar(lowerNibble);
	}

	// append a null after all the nibbles
	output[numNibbles] = '\0';

	return output;
}
/*
 * This method converts a hex string(character pointer) to a binary string.
 * Return true if the conversion was successful, false otherwise
 */
int sc_hex_to_bin(const char *in, char *out, size_t *outlen) {
	const int GENERAL_ERROR = 1;
	int err = 0;
	size_t left, count = 0;
	LOGD("cscdirectio: out put",in);

	if ((in == NULL) || (out == NULL) || (outlen == NULL))
	{
		return GENERAL_ERROR;
	}

	left = *outlen;
	LOGD("cscdirectio: out put",out);


	while (*in != '\0') {
		int byte = 0, nybbles = 2;

		while (nybbles-- && *in && *in != ':') {
			char c;
			byte <<= 4;
			c = *in++;
			if ('0' <= c && c <= '9')
				c -= '0';
			else if ('a' <= c && c <= 'f')
				c = c - 'a' + 10;
			else if ('A' <= c && c <= 'F')
				c = c - 'A' + 10;
			else {
				LOGD("cscdirectio:  invalid character");

				err = GENERAL_ERROR; //invalid character
				printf("invalid character\n");
				goto out;
			}
			byte |= c;
		}
		if (*in == ':')
			in++;
		if (left <= 0) {
			err = GENERAL_ERROR; //buffer too small
			printf("buffer too small\n");
			break;
		}
		out[count++] = (char) byte;
		left--;
	}

	out: *outlen = count;
	return err;
}

