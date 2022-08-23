/* 
* © 2004-2014, Tyfone, Inc. All rights reserved.
 * Patented and other Patents pending.
 * All trademarks are property of their respective owners.
 * SideTap, SideSafe, SideKey, The Connected Smart Card,
 * CSC are trademarks of Tyfone, Inc.
 * For questions visit: www.tyfone.com
 */

#define _GNU_SOURCE
#include <fcntl.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>
#include <sys/stat.h>
#include "stdbool.h"

#include "LowLevel.h"


static int wfileHandle = 0;
static int rfileHandle = 0;

static bool isConnected = false;

UCHAR readPattern[] = { 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
		0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50,
		0x51, 0x52, 0x53, 0x54, 0x55, };
//static int fd = 0, rfd = 0;
static bool isFdcreated = 0, isRFdcreated = 0;
/*
 * Reads 1 sector from the file at path into the buffer passed in 
 * The buffer must hold SECTOR_SIZE bytes
 * Returns 0 if successfully read SECTOR_SIZE bytes, or else error code
 */
int readSector(const char* path, int sector, PUCHAR buffer, size_t bufferSize) {

	UCHAR *b;
	  int rfd;
	int r;

#ifdef DEBUG
	LOGD("Tyfone SideTap LowLevel: reading %d bytes from file(%s)\n",
			SECTOR_SIZE, path);
#endif

	if (bufferSize < SECTOR_SIZE) {
		LOGE(
				"TyfoneSideTap LowLevel: readSector expected buffer of at least %d bytes, but bufferSize is only %d bytes\n",
				SECTOR_SIZE, bufferSize);

		return EINVAL;
	}

	b = memalign(1,SECTOR_SIZE);
	if (!b) {

		LOGE(
				"Tyfone SideTap LowLevel: readSector unable to allocate %d bytes memory\n",
				SECTOR_SIZE);

		return ENOMEM;
	}
	/*if(!isRFdcreated){
	 LOGE("\n isFdcreated %d",isRFdcreated);
	 // fd = open(path, O_RDONLY | O_DIRECT);
	 rfd = open(path, O_RDONLY | O_DIRECT);
	 if (rfd > 0){
	 isRFdcreated=1;
	 }
	 }*/
	rfd = open(path, O_RDONLY | O_DIRECT);
	if (rfd < 0) {

		LOGE("Tyfone SideTap LowLevel: unable to open file (%s) for reading\n",
				path);

		free(b);
		return errno;
	}

	// OS X does not support O_DIRECT, trying this instead
#ifdef __APPLE__
	fcntl(rfd, F_NOCACHE, 1);
#endif

	r = lseek(rfd, SECTOR_SIZE * sector, SEEK_SET);
	if (r != SECTOR_SIZE * sector) {
		if (r >= 0)
			errno = EINVAL;

		LOGE(
				"Tyfone SideTap LowLevel: readSector failed to seek to block (%d) in file (%s).  offset should be (%d), result of seek was (%d)\n",
				sector, path, SECTOR_SIZE * sector, r);

		free(b);
		close(rfd);
		return errno;
	}

	r = read(rfd, b, SECTOR_SIZE);
	if (r != SECTOR_SIZE) {

		LOGE(
				"Tyfone SideTap LowLevel: failed to read %d byte block from file (%s).  Result of read was (%d)\n",
				SECTOR_SIZE, path, r);

		if (r >= 0)
			errno = EINVAL;

		free(b);
		close(rfd);
		return errno;
	}

	// close(fd);

	// Copy bytes into buffer passed into function.
	// TODO: is this necessary? We should read directly into buffer received from caller
	memcpy(buffer, b, SECTOR_SIZE);

	free(b);
	close(rfd);
	return EXIT_SUCCESS;
}

int writeSector(const char* path, int sector, PUCHAR bytesToWrite,
		size_t numBytes) {

	UCHAR *b;
	int fd;
	int r;

#ifdef DEBUG
	LOGD("Tyfone SideTap LowLevel: writing %d bytes to %s\n", SECTOR_SIZE,
			path);
#endif

	if (!bytesToWrite) {

		LOGE(
				"Tyfone SideTap LowLevel: writeSector did not receive byte array as expected\n");

		return EINVAL;
	}

	if (numBytes > SECTOR_SIZE) {

		LOGE(
				"Tyfone SideTap LowLevel: size of byte array exceeds expected block size.  Received (%d) bytes, expected (%d) bytes\n",
				numBytes, SECTOR_SIZE);

		return EINVAL;
	}
	/*if(!isFdcreated){
	 LOGE("\n isFdcreated %d",isFdcreated);
	 fd = open(path, O_WRONLY | O_DIRECT | O_CREAT,S_IRUSR|S_IWUSR);
	 if (fd > 0){
	 isFdcreated=1;
	 }
	 }*/
	fd = open(path, O_WRONLY | O_DIRECT | O_CREAT, S_IRUSR | S_IWUSR);

	if (fd < 0) {

		LOGE(
				"Tyfone SideTap LowLevel: unable to create/open file (%s) for writing\n",
				path);

		return errno;
	}

	// OS X does not support O_DIRECT, trying this instead
#ifdef __APPLE__
	fcntl(fd, F_NOCACHE, 1);
#endif

	r = lseek(fd, SECTOR_SIZE * sector, SEEK_SET);
	if (r != SECTOR_SIZE * sector) {
		if (r >= 0)
			errno = EINVAL;

		LOGE(
				"Tyfone SideTap LowLevel: writeSector failed to seek to block (%d) in file (%s).  Offset should be (%d), result of seek was (%d)\n",
				sector, path, SECTOR_SIZE * sector, r);

		close(fd);
		return errno;
	}

	b = memalign(1,SECTOR_SIZE);
	if (!b) {

		LOGE(
				"Tyfone SideTap LowLevel: writeSector unable to allocate %d bytes memory\n",
				SECTOR_SIZE);

		close(fd);
		return ENOMEM;
	}
	memset(b, 0, SECTOR_SIZE);

	// copy the incoming byte array
	memcpy(b, bytesToWrite, numBytes);

	r = write(fd, b, SECTOR_SIZE);
	if (r != SECTOR_SIZE) {
		if (r >= 0)
			errno = EINVAL;

		LOGE(
				"Tyfone SideTap LowLevel: failed to write %d byte block to file (%s).  Result of write was (%d)\n",
				SECTOR_SIZE, path, r);

		close(fd);
		free(b);
		return errno;
	}
	free(b);
	close(fd);

	return EXIT_SUCCESS;
}

/**
 * Duplicates bytes passed in until they fill a buffer.
 * buffer - allocated array of bufferSize bytes
 * bufferSize - size of buffer i.e. sizeof(buffer)
 * bytesToDuplicate - array of bytes that will be copied into buffer multiple times until buffer is full.
 * sourceSize - size of bytesToDuplicate i.e. sizeof(bytesToDuplicate)
 * if the data passed in exceeds bufferSize, only the first bufferSizeBites will be copied to the buffer.
 */
void fillBuffer(UCHAR buffer[], size_t bufferSize, UCHAR bytesToDuplicate[],
		size_t sourceSize) {

	PUCHAR ptr = buffer;
	int remainingSpace = bufferSize;

	while (remainingSpace > 0) {

		// Copy the whole thing if there is space, or else just as much as fits.

		if (remainingSpace > sourceSize) {

			memcpy(ptr, bytesToDuplicate, sourceSize);

			ptr += sourceSize;
			remainingSpace -= sourceSize;

		} else {
			memcpy(ptr, bytesToDuplicate, remainingSpace);
			return;
		}
	}
}

int writeBytes(const char* path, PUCHAR bytesToWrite, size_t numBytes) {

	// reuse this buffer for each write
	static UCHAR buffer[SECTOR_SIZE];

	fillBuffer(buffer, sizeof buffer, bytesToWrite, numBytes);

	return writeSector(path, DEFAULT_SECTOR, buffer, SECTOR_SIZE);
}

/*
 * Writes a sector of dummy data into a file at the given path
 * Returns success or failure - return code
 */
int writeDummyData(const char* path) {

	return writeBytes(path, readPattern, sizeof readPattern);
}

/*
 * Checks the start of the given bytes to see if they seem to match 
 * the dummy data from the read file
 *
 * Assumes it is safe to read the first 4 bytes 
 */
int didReadDummyData(PUCHAR bytesFromSE) {
	const size_t numBytesToCheck = 4;

	return (memcmp(bytesFromSE, readPattern, numBytesToCheck) == 0);
}

/*
 * Reads array of bytes of data as a response from the SmartMx
 * Throws an IOException if there is any problem.
 * param file
 *  file from which dummy date will be read
 * returns 
 *    Array of bytes of response data from the SE.
 *    null pointer if read fails 
 */
int readBytes(const char* path, PUCHAR buffer, size_t bufferSize) {

	return readSector(path, DEFAULT_SECTOR, buffer, bufferSize);
}

/*int getCardPresence(){
 writeDummyData()
 }*/
void closeFiledescriptor() {
//	LOGE("Closing filedescriptor");
//	if (fd > 0)
//		close(fd);
//	if (rfd > 0)
//		close(rfd);
}

void createFiledescriptor(const char* path) {
//	closeFiledescriptor();
//	fd = open(path, O_WRONLY | O_DIRECT | O_CREAT, S_IRUSR | S_IWUSR);
//	if (fd > 0) {
//		isFdcreated = 1;
//	}
//	rfd = open(path, O_RDONLY | O_DIRECT);
//	if (rfd > 0) {
//		isRFdcreated = 1;
//	}
}
int isFileExists(char* fname) {

	LOGE("\nisFileExists (%s): FALSE", fname);

	/*
	 UCHAR dummyPattern[] = { 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55 };

	 static UCHAR buffer[SECTOR_SIZE];

	 fillBuffer(buffer, sizeof buffer, dummyPattern, sizeof(dummyPattern));

	 return writeSector(wfileHandle, DEFAULT_SECTOR, buffer, SECTOR_SIZE);*/

	if (access(fname, F_OK) != -1) {
		//LOGE("\nisFileExists (%s): TRUE",fname);
		// file exists
		if (isConnected == false)
			createTyfoneFile(fname);

		return EXIT_SUCCESS;
	} else {
		// file doesn't exist
		LOGE("\nisFileExists (%s): FALSE", fname);
		createTyfoneFile(fname);
		return EXIT_FAILURE;
	}
}
int createTyfoneFile(const char* path) {
	if (path == "")
		return EXIT_FAILURE;

	if (wfileHandle)
		close(wfileHandle);

	if (rfileHandle)
		close(rfileHandle);

	wfileHandle = 0;
	rfileHandle = 0;

	wfileHandle = open(path, O_WRONLY | O_DIRECT | O_CREAT, S_IRUSR | S_IWUSR);

	rfileHandle = open(path, O_RDONLY | O_DIRECT);

	if (wfileHandle > 0) {
		//fprintf(stderr, "\nInside createTyfoneFile : EXIT_SUCCESS\nPath: %s",path);
		isConnected = true;
		return EXIT_SUCCESS;
	} else {
		//fprintf(stderr, "\nInside createTyfoneFile : EXIT_FAILURE\nPath: %s",path);
		isConnected = false;
		return EXIT_FAILURE;
	}
}

void closeTyfoneFile() {
	if (!wfileHandle)
		return;

	close(wfileHandle);
	wfileHandle = 0;

	close(rfileHandle);
	rfileHandle = 0;

	isConnected = false;
}
int checkFileHandle() {
	UCHAR dummyPattern[] = { 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
			0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
			0x49, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55 };

	static UCHAR buffer[SECTOR_SIZE];

	fillBuffer(buffer, sizeof buffer, dummyPattern, sizeof(dummyPattern));

	return writeSector(wfileHandle, DEFAULT_SECTOR, buffer, SECTOR_SIZE);
}
