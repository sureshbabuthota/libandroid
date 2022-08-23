/* 
* © 2004-2014, Tyfone, Inc. All rights reserved.
* Patented and other Patents pending.  
* All trademarks are property of their respective owners. 
* SideTap, SideSafe, SideKey, The Connected Smart Card,  
* CSC are trademarks of Tyfone, Inc. 
* For questions visit: www.tyfone.com 
*/ 

#include <sys/types.h>

#include "internal/types.h"

#ifndef _LowLevel_h_
#define _LowLevel_h_

#ifdef __cplusplus
extern "C" {
#endif 
  
// OS X does not include the O_DIRECT symbol
#ifdef __APPLE__
#define O_DIRECT 0
#endif

#define SECTOR_SIZE 512
#define DEFAULT_SECTOR 0

/*
 * Logging macros.  Undefine DEBUG to remove logging for levels less than W
 */

#define DEBUG


#define LOGV(...) ((void)fprintf(stderr, __VA_ARGS__)) 
#define LOGD(...) ((void)fprintf(stderr, __VA_ARGS__))
#define LOGI(...) ((void)fprintf(stderr, __VA_ARGS__))
#define LOGW(...) ((void)fprintf(stderr, __VA_ARGS__))
#define LOGE(...) ((void)fprintf(stderr, __VA_ARGS__))
 
//extern char* getPath();
//extern void setPath(char* newPath);

//extern int writeSector(const char* path, int sector, PUCHAR bytesToWrite, size_t numBytes);
//extern int readSector(const char* path, int sector, PUCHAR buffer, size_t bufferSize);
int writeBytes(const char* path, PUCHAR bytesToWrite, size_t numBytes);
int readBytes(const char* path, PUCHAR buffer, size_t bufferSize);
int checkFileHandle();
int createTyfoneFile(const char* fname);
void closeTyfoneFile();
int isFileExists(char* fname);
extern void fillBuffer(UCHAR buffer[], size_t bufferSize, 
    UCHAR bytesToDuplicate[], size_t sourceSize);

extern int writeDummyData(const char* path);
int didReadDummyData(PUCHAR bytesFromSE); 

void closeFiledescriptor();
void createFiledescriptor(const char* path);
//int getCardPresence();
#ifdef __cplusplus
}
#endif 

#endif /* _LowLevel_h_ */


