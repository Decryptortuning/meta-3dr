SUMMARY = "SoloLink software for RC, telemetry and video"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://LICENSE-APACHE;md5=3b83ef96387f14655fc854ddc3c6bd57"

SRCREV = "1cd50aa3707d37d7ce8c34e5f85fdfbd08c9770a"
SRC_URI = "git://github.com/Decryptortuning/sololink.git;protocol=https;branch=codex/mavlink-modernization"

PV = "0.0.0"
PKGV = "${PV}+git${GITPKGV}"
S = "${WORKDIR}/git"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

inherit pkgconfig gitpkgv

PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS += "libnl pymavlink"
DEPENDS += "gstreamer1.0"

RDEPENDS:${PN} += "dronekit"
RDEPENDS:${PN} += "bash"
RDEPENDS:${PN} += "imx-vpu imx-vpuwrap gstreamer1.0 imx-gst1.0-plugin"
RDEPENDS:${PN} += "python3-core python3-json python3-io python3-pyserial python3-datetime python3-fcntl python3-threading python3-syslog"

# sololink ships a customized /etc/init.d/networking.
RDEPENDS:${PN}-config = "init-ifupdown busybox-syslog bash python3-core python3-json python3-io python3-crypt python3-shell"

# These need to match the environment variables in Sololink/config/sololink
# All should come from the machine configuration meta-3dr/conf/machine/*
soloconfdir = "${SOLOLINK_CONFIG_DIR}"

# Config/init and drone LED helpers have separate package ownership.
PACKAGES =+ "${PN}-config ${PN}-led"
FILES:${PN}-config = "${bindir}/opensolo_wifi.py ${sysconfdir}/init.d ${sysconfdir}/rcS.d ${sysconfdir}/rc0.d ${sysconfdir}/rc3.d ${sysconfdir}/rc6.d ${sysconfdir}/profile.d/sololink ${soloconfdir}/sololink.orig ${sysconfdir}/logrotate-streams.conf ${sysconfdir}/logrotate-sololink.conf"
FILES:${PN}-led = "${bindir}/led_control.py ${bindir}/SoloLED.py"
FILES:${PN} = "${bindir}/* ${sbindir}/opensolo-log-provision"
RDEPENDS:${PN} += "${PN}-config ${PN}-led"
RDEPENDS:${PN}-led = "python3-core dronekit"
OPENSOLO_ROLE:imx_controller = "controller"
OPENSOLO_ROLE:imx_drone = "solo"

# This is for the builds in flightcode. Building with the --sysroot flag
# causes the preprocessor to find the correct include files if they are on
# the default include path (relative to sysroot/usr/include), but using
# -I to add additional include paths does not work; the full path with
# sysroot has to be used with -I.
do_compile:prepend () {
	export OECORE_TARGET_SYSROOT="${STAGING_DIR_TARGET}"
}

do_install () {
	install -d ${D}${bindir}
	install -d ${D}/

	install -d ${D}${sysconfdir}
	install -d ${D}${sysconfdir}/init.d
	install -d ${D}${sysconfdir}/rcS.d
	install -d ${D}${sysconfdir}/rc0.d
	install -d ${D}${sysconfdir}/rc3.d
	install -d ${D}${sysconfdir}/rc6.d

	install -m 0755 ${S}/flightcode/python/stm32_defs.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/python/lsproc.py ${D}${bindir}

	install -m 0755 ${S}/px_uploader/px_uploader.py ${D}${bindir}
	install -m 0755 ${S}/px_uploader/loadPixhawk.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/python/pixhawk.py ${D}${bindir}
	install -m 0644 ${S}/flightcode/python/gpio.py ${D}${bindir}
	install -m 0644 ${S}/flightcode/python/led_solo.py ${D}${bindir}/led.py
	install -m 0644 ${S}/flightcode/python/usb_solo.py ${D}${bindir}/usb.py
    install -m 0755 ${S}/flightcode/python/led_control.py ${D}${bindir}
    install -m 0755 ${S}/flightcode/python/SoloLED.py ${D}${bindir}
	install -m 0755 ${S}/init/pixhawk ${D}${sysconfdir}/init.d
	# link rcS.d/S60pixhawk -> ../init.d/pixhawk created by the machine override below

	install -m 0755 ${S}/STM32Loader/updateArtoo.sh ${D}${bindir}
	install -m 0755 ${S}/STM32Loader/checkArtooAndUpdate.py ${D}${bindir}
	install -m 0755 ${S}/STM32Loader/reset_artoo ${D}${bindir}

	install -m 0755 ${S}/init/shutdownArtoo.sh ${D}${bindir}
	ln -sf ../../usr/bin/shutdownArtoo.sh ${D}${sysconfdir}/rc0.d/S89shutdownArtoo
    
    install -m 0755 ${S}/init/golden_to_system.sh ${D}${sysconfdir}/init.d
    # Validated initramfs seeding owns GOLDEN -> LATEST; legacy migration is not autostarted.

    install -m 0755 ${S}/init/clock_sync ${D}${bindir}/clock_sync
    install -m 0755 ${S}/init/clock_sync.init ${D}${sysconfdir}/init.d/clock_sync
    install -m 0755 ${S}/init/log_maintenance ${D}${bindir}/log_maintenance
    install -m 0755 ${S}/net/usr/bin/sololink-ap-service ${D}${bindir}
    install -m 0755 ${S}/net/usr/bin/sololink-lan-firewall ${D}${bindir}
    install -m 0644 ${S}/config/logrotate-streams.conf ${D}${sysconfdir}

	install -m 0755 ${S}/flightcode/video/app/app_streamer ${D}${bindir}
	install -m 0755 ${S}/flightcode/video/vid/vidlaunch ${D}${bindir}
	install -m 0755 ${S}/flightcode/video/hdmi/hdmiout ${D}${bindir}
	install -m 0755 ${S}/flightcode/video/cleanLibs.sh ${D}${bindir}
    # No automatic deletion of legacy overlay library paths.

	install -m 0755 ${S}/net/etc/init.d/netinit ${D}${sysconfdir}/init.d
	install -m 0755 ${S}/net/etc/init.d/networking ${D}${sysconfdir}/init.d
	ln -sf ../init.d/netinit ${D}${sysconfdir}/rcS.d/S41netinit
	ln -sf ../init.d/networking ${D}${sysconfdir}/rcS.d/S41networking

	install -m 0755 ${S}/net/usr/bin/clock.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/configfile.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/getmaclocal.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/hostapdconfig.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/ip.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/iw.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/wpa_supplicant.py ${D}${bindir}

	install -m 0755 ${S}/pair/pair.py ${D}${bindir}
	install -m 0755 ${S}/pair/pair_server.py ${D}${bindir}
	install -m 0755 ${S}/pair/pair_button.py ${D}${bindir}
	install -m 0755 ${S}/pair/pair_confirm.py ${D}${bindir}
	install -m 0755 ${S}/pair/pair_solo.py ${D}${bindir}
	install -m 0755 ${S}/pair/hostapd_ctrl.py ${D}${bindir}
	install -m 0755 ${S}/pair/ifconfig.py ${D}${bindir}
	install -m 0755 ${S}/pair/ip_util.py ${D}${bindir}
	install -m 0755 ${S}/pair/runlevel.py ${D}${bindir}
	install -m 0755 ${S}/pair/udhcpc.py ${D}${bindir}
	install -m 0755 ${S}/pair/wpa_cli.py ${D}${bindir}
	install -m 0755 ${S}/pair/wpa_control.py ${D}${bindir}

	install -m 0755 ${S}/net/usr/bin/rc_cli.py ${D}${bindir}
	install -m 0755 ${S}/net/usr/bin/rc_remap_sample.py ${D}${bindir}

	install -m 0755 ${S}/flightcode/telem_ctrl/telem_ctrl ${D}${bindir}

	install -m 0755 ${S}/flightcode/python/app_server.py ${D}${bindir}

	install -d ${D}${sysconfdir}/profile.d

    # Write build-specific profile content in the install tree, never mutate S.
    install -m 0644 ${S}/config/sololink ${D}${sysconfdir}/profile.d
    echo "export SOLOLINK_CONFIG_DIR=${SOLOLINK_CONFIG_DIR}" >> ${D}${sysconfdir}/profile.d/sololink
    echo "alias ls='ls -aF'" >> ${D}${sysconfdir}/profile.d/sololink
    echo "alias ll='ls -l'" >> ${D}${sysconfdir}/profile.d/sololink
    echo "alias hi='history'" >> ${D}${sysconfdir}/profile.d/sololink

	# Only the .orig file is installed; it is the "golden config" that
	# configinit uses to create sololink.conf (and md5) on first boot
	install -m 0644 ${S}/config/sololink.orig ${D}${soloconfdir}

	install -m 0755 ${S}/config/max_dgram_qlen ${D}${sysconfdir}/init.d
	ln -sf ../init.d/max_dgram_qlen ${D}${sysconfdir}/rcS.d/S10max_dgram_qlen

	install -m 0755 ${S}/config/configinit ${D}${sysconfdir}/init.d
	ln -sf ../init.d/configinit ${D}${sysconfdir}/rcS.d/S41configinit

	install -m 0755 ${S}/config/checknet ${D}${sysconfdir}/init.d
	ln -sf ../init.d/checknet ${D}${sysconfdir}/rc3.d/S01checknet

	install -m 0755 ${S}/init/startwd ${D}${sysconfdir}/init.d
	ln -sf ../init.d/startwd ${D}${sysconfdir}/rc6.d/S80startwd

	install -m 0755 ${S}/config/sololink_config ${D}${bindir}
    install -m 0755 ${S}/config/opensolo_wifi.py ${D}${bindir}

	install -m 0755 ${S}/wifi/wifistats ${D}${bindir}
	install -m 0755 ${S}/wifi/survey_dump ${D}${bindir}
	install -m 0755 ${S}/wifi/survey_log ${D}${bindir}
	install -m 0755 ${S}/wifi/logRCUp.sh ${D}${bindir}

	install -m 0755 ${S}/flightcode/telem/telem_forwarder ${D}${bindir}
	install -m 0755 ${S}/flightcode/rssi/rssi_send ${D}${bindir}
	install -m 0755 ${S}/flightcode/pixrc/pixrc ${D}${bindir}
	install -m 0755 ${S}/flightcode/stm32/stm32 ${D}${bindir}
	install -m 0755 ${S}/flightcode/tlog/tlog ${D}${bindir}
	install -m 0755 ${S}/flightcode/proc_top/proc_top ${D}${bindir}
	install -m 0755 ${S}/flightcode/thermal/log_temp ${D}${bindir}

	# busybox-syslog owns its configuration and early service registration.

	# rotate 3dr logs each boot
	install -m 0755 ${S}/init/3dr_rotate ${D}${sysconfdir}/init.d
	ln -sf ../init.d/3dr_rotate ${D}${sysconfdir}/rcS.d/S433dr_rotate

	# Machine-specific log rotation is installed below.

	# Inject updater screen messages
	install -m 0755 ${S}/flightcode/stm32/updater_msg.py ${D}${bindir}

	# Inject lockout screen messages
	install -m 0755 ${S}/flightcode/stm32/lockout_msg.py ${D}${bindir}

	# Inject app connected messages
	install -m 0755 ${S}/flightcode/stm32/app_connected_msg.py ${D}${bindir}

	# Input report message test/demo
	install -m 0755 ${S}/flightcode/stm32/input_report_client.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/stm32/input_report_msg.py ${D}${bindir}

	# Button event message test/demo
	install -m 0755 ${S}/flightcode/stm32/btn_client.py ${D}${bindir}

	# Stick axes configuration
	install -m 0644 ${S}/flightcode/stm32/config_stick_axes_msg.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/stm32/config_stick_axes.py ${D}${bindir}

	# Camera tilt sweep dial configuration
	install -m 0644 ${S}/flightcode/stm32/config_sweep_time_msg.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/stm32/config_sweep_time.py ${D}${bindir}

	# Param stored values message
	install -m 0755 ${S}/flightcode/stm32/param_stored_vals_msg.py ${D}${bindir}

	# Telemetry units configuration
	install -m 0644 ${S}/flightcode/stm32/set_telem_units_msg.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/stm32/set_telem_units.py ${D}${bindir}

	# Watchdog support
	install -m 0755 ${S}/flightcode/wdog/wdog ${D}${bindir}

	# Stick calibration for artoo
	install -m 0755 ${S}/flightcode/python/stick-cal.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/python/slip.py ${D}${bindir}
	install -m 0755 ${S}/flightcode/python/runStickCal.sh ${D}${bindir}

	#dataflash log downloading
	install -m 0755 ${S}/flightcode/dflog/dflog ${D}${bindir}
	install -m 0755 ${S}/flightcode/dflog/loadLog.py ${D}${bindir}

	#mavlink-downloaded-dataflash log transfers to artoo
	install -m 0755 ${S}/flightcode/dflog/dataFlashMAVLink-to-artoo.py ${D}${bindir}

	# the logger module is responsible for initiating and handling
	#          dataflash transfers from the pixhawk:
	#echo "module load logger" >> ${D}/.mavinit.scr

	# the logger binary is responsible for initiating and handling
	#          dataflash transfers from the pixhawk:
	install -m 0755 ${S}/flightcode/dataflash_logger/dataflash_logger ${D}${bindir}

	#Solo wifi production test
	install -m 0755 ${S}/tools/wifiTest/SoloTest.sh ${D}${bindir}

	#RC unlock
	install -m 0755 ${S}/flightcode/unlock/unlock ${D}${bindir}
	install -m 0755 ${S}/flightcode/python/rc_lock.py ${D}${bindir}

	#Gimbal
	install -d ${D}${bindir}/gimbal
	install -m 0644 gimbal/firmware_helper.py         ${D}${bindir}/gimbal
	install -m 0644 gimbal/firmware_loader.py         ${D}${bindir}/gimbal
	install -m 0644 gimbal/setup_comutation.py        ${D}${bindir}/gimbal
	install -m 0644 gimbal/setup_mavlink.py           ${D}${bindir}/gimbal
	install -m 0644 gimbal/setup_param.py             ${D}${bindir}/gimbal
	install -m 0644 gimbal/setup_factory_pub.py       ${D}${bindir}/gimbal
	install -m 0644 gimbal/setup_validate.py          ${D}${bindir}/gimbal
	install -m 0755 gimbal/setup.py                   ${D}${bindir}/gimbal
	# put this in gimbal subdir so as not to conflict with existing one
	ln -sf /usr/bin/gimbal/setup.py                   ${D}${bindir}/gimbal_setup
	install -m 0755 gimbal/loadGimbal.py              ${D}${bindir}
	install -m 0755 ${S}/init/updateGimbal.sh         ${D}${sysconfdir}/init.d
	# link rcS.d/S62updateGimbal.sh -> ../init.d/updateGimbal.sh created by the machine override below
}


do_install:append:imx_controller() {
    sed -i 's/^ApEnable=.*/ApEnable=True/; s/^StationEnable=.*/StationEnable=False/' ${D}${soloconfdir}/sololink.orig
    printf '%s\n' '#!/bin/sh' '/usr/bin/checkArtooAndUpdate.py' > ${D}${sysconfdir}/init.d/updateArtoo
    chmod 0755 ${D}${sysconfdir}/init.d/updateArtoo
    ln -sf ../init.d/updateArtoo ${D}${sysconfdir}/rcS.d/S60updateArtoo.sh
}
do_install:append:imx_drone() {
    sed -i 's/^ApEnable=.*/ApEnable=False/; s/^StationEnable=.*/StationEnable=True/' ${D}${soloconfdir}/sololink.orig
    ln -sf ../init.d/pixhawk ${D}${sysconfdir}/rcS.d/S60pixhawk
    ln -sf ../init.d/updateGimbal.sh ${D}${sysconfdir}/rcS.d/S62updateGimbal.sh
    rm -f ${D}${sysconfdir}/rc0.d/S89shutdownArtoo
}

RDEPENDS:${PN} += "python3-core python3-io python3-shell python3-json python3-logging python3-netclient openssh-keygen openssh-ssh rsync"


do_install:append() {
    install -m 0644 ${S}/config/logrotate-sololink.conf.${OPENSOLO_ROLE} ${D}${sysconfdir}/logrotate-sololink.conf
    ln -sf ../init.d/clock_sync ${D}${sysconfdir}/rcS.d/S50clock_sync
    ln -sf ../init.d/clock_sync ${D}${sysconfdir}/rc0.d/K20clock_sync
    ln -sf ../init.d/clock_sync ${D}${sysconfdir}/rc6.d/K20clock_sync
    install -d ${D}${sbindir}
    install -m 0755 ${S}/flightcode/dflog/opensolo-log-provision ${D}${sbindir}/opensolo-log-provision
}
