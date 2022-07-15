#
# Audio Package 2
#
# Include this file in a product makefile to include these audio files
#
# This is a larger package of sounds than the 1.0 release for devices
# that have larger internal flash.
#

LOCAL_PATH:= frameworks/base/data/sounds

PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/F1_MissedCall.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/F1_MissedCall.ogg \
	$(LOCAL_PATH)/F1_New_MMS.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/F1_New_MMS.ogg \
	$(LOCAL_PATH)/F1_New_SMS.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/F1_New_SMS.ogg \
	$(LOCAL_PATH)/Alarm_Buzzer.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Alarm_Buzzer.ogg \
	$(LOCAL_PATH)/Alarm_Beep_01.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Alarm_Beep_01.ogg \
	$(LOCAL_PATH)/Alarm_Beep_02.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Alarm_Beep_02.ogg \
	$(LOCAL_PATH)/Alarm_Classic.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Alarm_Classic.ogg \
	$(LOCAL_PATH)/Alarm_Beep_03.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Alarm_Beep_03.ogg \
	$(LOCAL_PATH)/Alarm_Rooster_02.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Alarm_Rooster_02.ogg \
	$(LOCAL_PATH)/Ring_Classic_02.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Ring_Classic_02.ogg \
	$(LOCAL_PATH)/Ring_Digital_02.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Ring_Digital_02.ogg \
	$(LOCAL_PATH)/Ring_Synth_04.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Ring_Synth_04.ogg \
	$(LOCAL_PATH)/Ring_Synth_02.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Ring_Synth_02.
	$(LOCAL_PATH)/notifications/Heaven.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/Heaven.ogg \
	$(LOCAL_PATH)/notifications/Tinkerbell.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/Tinkerbell.ogg \
	$(LOCAL_PATH)/effects/Effect_Tick.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/Effect_Tick.ogg \
	$(LOCAL_PATH)/effects/KeypressStandard.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/KeypressStandard.ogg \
	$(LOCAL_PATH)/effects/KeypressSpacebar.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/KeypressSpacebar.ogg \
	$(LOCAL_PATH)/effects/KeypressDelete.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/KeypressDelete.ogg \
	$(LOCAL_PATH)/effects/ogg/KeypressInvalid.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/KeypressInvalid.ogg \
	$(LOCAL_PATH)/effects/KeypressReturn.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/KeypressReturn.ogg \
	$(LOCAL_PATH)/effects/VideoRecord.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/VideoRecord.ogg \
	$(LOCAL_PATH)/effects/VideoStop.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/VideoStop.ogg \
	$(LOCAL_PATH)/effects/camera_click.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/camera_click.ogg \
	$(LOCAL_PATH)/effects/LowBattery.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/LowBattery.ogg \
	$(LOCAL_PATH)/effects/Dock.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/Dock.ogg \
	$(LOCAL_PATH)/effects/Undock.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/Undock.ogg \
	$(LOCAL_PATH)/effects/Lock.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/Lock.ogg \
	$(LOCAL_PATH)/effects/Unlock.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/Unlock.ogg \
	$(LOCAL_PATH)/effects/ogg/Trusted.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui/Trusted.ogg \
	$(LOCAL_PATH)/notifications/moonbeam.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/moonbeam.ogg \
	$(LOCAL_PATH)/notifications/pixiedust.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/pixiedust.ogg \
	$(LOCAL_PATH)/notifications/pizzicato.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/pizzicato.ogg

ifneq ($(MINIMAL_NEWWAVELABS),true)
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/newwavelabs/GameOverGuitar.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/GameOverGuitar.ogg \
	$(LOCAL_PATH)/newwavelabs/MidEvilJaunt.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/MidEvilJaunt.ogg \
	$(LOCAL_PATH)/newwavelabs/MildlyAlarming.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/MildlyAlarming.ogg \
	$(LOCAL_PATH)/newwavelabs/NewPlayer.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/NewPlayer.ogg \
	$(LOCAL_PATH)/newwavelabs/Noises1.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Noises1.ogg \
	$(LOCAL_PATH)/newwavelabs/Noises2.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Noises2.ogg \
	$(LOCAL_PATH)/newwavelabs/Noises3.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Noises3.ogg \
	$(LOCAL_PATH)/newwavelabs/SitarVsSitar.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/SitarVsSitar.ogg \
	$(LOCAL_PATH)/newwavelabs/SpringyJalopy.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/SpringyJalopy.ogg \
	$(LOCAL_PATH)/newwavelabs/DearDeer.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/DearDeer.ogg \
	$(LOCAL_PATH)/newwavelabs/DontPanic.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/DontPanic.ogg \
	$(LOCAL_PATH)/newwavelabs/Voila.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/Voila.ogg \
	$(LOCAL_PATH)/newwavelabs/CrazyDream.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/CrazyDream.ogg \
	$(LOCAL_PATH)/newwavelabs/DreamTheme.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/DreamTheme.ogg \
	$(LOCAL_PATH)/newwavelabs/Champagne_Edition.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Champagne_Edition.ogg \
	$(LOCAL_PATH)/newwavelabs/Eastern_Sky.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Eastern_Sky.ogg \
	$(LOCAL_PATH)/newwavelabs/Revelation.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Revelation.ogg \
	$(LOCAL_PATH)/newwavelabs/Paradise_Island.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/Paradise_Island.ogg
endif