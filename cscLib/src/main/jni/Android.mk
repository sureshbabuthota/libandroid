

##################
# pcsc-jni: a jni interface for accessing PCSC-Lite
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := cscdirectio

LOCAL_SRC_FILES := \
			cscdirectio.c \
			SD2SIM.c \
			LowLevel.c 
			
LOCAL_C_INCLUDES :=\
	$(LOCAL_PATH)/internal 
			
LOCAL_LDFLAGS += -ldl -llog
LOCAL_LDLIBS := -llog


include $(BUILD_SHARED_LIBRARY)
##################

