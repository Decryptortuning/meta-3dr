include recipes-core/images/core-image-base.bb

# created from image_types_fsl; creates additional logging partition
inherit image_types_3dr

IMAGE_FEATURES:remove = "debug-tweaks allow-empty-password empty-root-password"

do_image_sdcard[depends] += "parted-native:do_populate_sysroot \
                             dosfstools-native:do_populate_sysroot \
                             mtools-native:do_populate_sysroot \
                             virtual/kernel:do_deploy"

PV = "3.0.0"
VER_NAME = "OpenSolo 5 development"
BUILD_DATE = "Build Date: ${@time.strftime('%Y%m%d%H%M%S', time.gmtime())}"

# Alias target name (avoid a second image recipe that would collide in deploy/).
PROVIDES += "solo_controller"

do_rootfs[depends] += "virtual/kernel:do_bundle_initramfs"

IMAGE_FEATURES += " \
    package-management \
"

# Add extra image features
EXTRA_IMAGE_FEATURES += " \
    ssh-server-openssh \
"

IMAGE_INSTALL += " \
    fsl-rc-local \
    imx-gst1.0-plugin \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-base-app \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-good-udp \
    gstreamer1.0-plugins-good-rtp \
    gstreamer1.0-plugins-bad-mpegtsdemux \
    gstreamer1.0-plugins-bad-videoparsersbad \
    gstreamer1.0-libav \
    gstreamer1.0 \
    v4l-utils \
    libudev \
    python3-core \
    python3-modules \
    python3-pip \
    python3-numpy \
    python3-posix-ipc \
    python3-monotonic \
    alsa-ucm-conf \
    openssh \
    iptables \
    iw \
    wireless-tools \
    hostapd \
    dnsmasq \
    sololink \
    sololink-python \
    util-linux \
    artoo-firmware \
    e2fsprogs-e2fsck \
    dosfstools \
    nano \
    vim \
    openssh-sftp-server \
    3dr-splash \
    persist-logs \
    rsync \
    stm32loader \
"

update_config_files() {
    # update /etc/network/interfaces
    mv ${IMAGE_ROOTFS}/etc/network/interfaces-controller \
            ${IMAGE_ROOTFS}/etc/network/interfaces
    rm ${IMAGE_ROOTFS}/etc/network/interfaces-solo
    # AP enabled by default, Station disabled
    sed -i 's/^ApEnable=.*/ApEnable=True/' ${IMAGE_ROOTFS}/etc/sololink.orig
    sed -i 's/^StationEnable=.*/StationEnable=False/' ${IMAGE_ROOTFS}/etc/sololink.orig
    # Create golden config files
    mv ${IMAGE_ROOTFS}/etc/hostapd.conf ${IMAGE_ROOTFS}/etc/hostapd.orig
    mv ${IMAGE_ROOTFS}/etc/wpa_supplicant.conf ${IMAGE_ROOTFS}/etc/wpa_supplicant.orig
    # Change hostname so solo and controller are different
    echo "3dr_controller" > ${IMAGE_ROOTFS}/etc/hostname
    # Provide /usr/bin/python for any remaining legacy shebangs
    ln -sf python3 ${IMAGE_ROOTFS}/usr/bin/python
    #Filesystem available over USB OTG port
    # Never export the locally writable LOG block device through USB mass storage.
    #Clear out the leases file on boot
    sed -i '/test \-d \/var\/lib\/misc\/.*/a \        rm -f \/var\/lib\/misc\/dnsmasq\.leases' ${IMAGE_ROOTFS}/etc/init.d/dnsmasq
    # Mount logging partition
    mkdir ${IMAGE_ROOTFS}/log
    echo "/dev/mmcblk0p4 /log auto defaults 0 2" >> ${IMAGE_ROOTFS}/etc/fstab

    # Persist syslog/klogd output to the LOG partition so reboot loops can be
    # debugged after the fact by inspecting /log/messages on the host.
    if [ -f ${IMAGE_ROOTFS}/etc/syslog-startup.conf ]; then
        sed -i 's#^LOGFILE=.*#LOGFILE=/log/messages#' ${IMAGE_ROOTFS}/etc/syslog-startup.conf
        # Ensure rotation is enabled (busybox syslogd -s/-b options).
        if ! grep -q '^ROTATESIZE=' ${IMAGE_ROOTFS}/etc/syslog-startup.conf; then
            echo "ROTATESIZE=10000\t\t# rotate log if grown beyond X [kByte]" >> ${IMAGE_ROOTFS}/etc/syslog-startup.conf
        fi
        if ! grep -q '^ROTATEGENS=' ${IMAGE_ROOTFS}/etc/syslog-startup.conf; then
            echo "ROTATEGENS=1\t\t\t# keep X generations of rotated logs" >> ${IMAGE_ROOTFS}/etc/syslog-startup.conf
        fi
        if ! grep -q '^LOGLEVEL=' ${IMAGE_ROOTFS}/etc/syslog-startup.conf; then
            echo "LOGLEVEL=7\t\t\t# local log level (between 1 and 8)" >> ${IMAGE_ROOTFS}/etc/syslog-startup.conf
        fi
    else
        cat > ${IMAGE_ROOTFS}/etc/syslog-startup.conf <<'EOF'
# This configuration file is used by the busybox syslog init script,
# /etc/init.d/syslog to set syslog configuration at start time.

DESTINATION=file		# log destinations (buffer file remote)
LOGFILE=/log/messages		# where to log (file)
REMOTE=loghost:514		# where to log (syslog remote)
REDUCE=no			# reduce-size logging
DROPDUPLICATES=no		# whether to drop duplicate log entries
ROTATESIZE=10000		# rotate log if grown beyond X [kByte]
ROTATEGENS=1			# keep X generations of rotated logs
BUFFERSIZE=64			# size of circular buffer [kByte]
FOREGROUND=no			# run in foreground (don't use!)
LOGLEVEL=7			# local log level (between 1 and 8)
EOF
    fi
    # Blacklist the Golden partition from udev
    echo "/dev/mmcblk0p1" >> ${IMAGE_ROOTFS}/etc/udev/mount.blacklist
    # Put a "Version" file in the root partition
    echo "${PV}" >> ${IMAGE_ROOTFS}/VERSION
    echo ${IMAGE_NAME} >> ${IMAGE_ROOTFS}/VERSION
    echo ${VER_NAME} >> ${IMAGE_ROOTFS}/VERSION
    echo ${BUILD_DATE} >> ${IMAGE_ROOTFS}/VERSION
    
    #Check the artoo version at boot and update if necessary
    #Always run this; it is what clears the "updating system" screen
    echo "#!/bin/sh" > ${IMAGE_ROOTFS}/etc/rcS.d/S60updateArtoo.sh
    echo "/usr/bin/checkArtooAndUpdate.py" >> ${IMAGE_ROOTFS}/etc/rcS.d/S60updateArtoo.sh
    chmod +x ${IMAGE_ROOTFS}/etc/rcS.d/S60updateArtoo.sh
    #1MB max rx socket buffer for video
    echo "net.core.rmem_max=1048576" >> ${IMAGE_ROOTFS}/etc/sysctl.conf

    # -----------------------------------------------------------------------------
    # Time / RTC handling
    # -----------------------------------------------------------------------------
    #
    # Many controllers are power-cut (not cleanly shutdown), and some boards do
    # not have a stable, battery-backed RTC. When the RTC contains an invalid
    # date, hwclock --hctosys can fail with "settimeofday() failed: Invalid argument"
    # early in boot.
    #
    # For log sanity, prefer /etc/timestamp (updated periodically) rather than
    # attempting to set from RTC each boot.
    if grep -q '^HWCLOCKACCESS=' ${IMAGE_ROOTFS}/etc/default/rcS; then
        sed -i 's/^HWCLOCKACCESS=.*/HWCLOCKACCESS=no/' ${IMAGE_ROOTFS}/etc/default/rcS
    else
        echo "HWCLOCKACCESS=no" >> ${IMAGE_ROOTFS}/etc/default/rcS
    fi

    # Ensure /etc/timestamp exists with a sane value (bootmisc uses this to
    # keep time monotonically increasing across power cuts).
    echo "${@time.strftime('%Y%m%d%H%M%S', time.gmtime())}" > ${IMAGE_ROOTFS}/etc/timestamp

    # Managed checkpoint service is installed by sololink.
    ln -sf ../init.d/clock_sync ${IMAGE_ROOTFS}/etc/rcS.d/S50clock_sync
    ln -sf ../init.d/clock_sync ${IMAGE_ROOTFS}/etc/rc0.d/K20clock_sync
    ln -sf ../init.d/clock_sync ${IMAGE_ROOTFS}/etc/rc6.d/K20clock_sync
    # A dedicated early-boot tmpfs cannot obscure retained /mnt child mounts.
    sed -i 's#^PSPLASH_FIFO_DIR=.*#PSPLASH_FIFO_DIR=/psplash#' ${IMAGE_ROOTFS}/etc/default/rcS

    # -----------------------------------------------------------------------------
    # ALSA UCM
    # -----------------------------------------------------------------------------
    #
    # alsactl uses ALSA UCM (Use Case Manager) when available. The upstream
    # alsa-ucm-conf package does not ship a profile for the controller's
    # HDMI sound card, so alsactl prints:
    #   snd_use_case_mgr_open error: failed to import hw:0 use case configuration
    #
    # Provide a minimal UCM2 profile for "imx-hdmi-soc" to avoid that warning.
    # Note: store the UCM files as standalone layer files (rather than here-docs)
    # to avoid bitbake parser confusion with braces inside shell functions.
    install -d ${IMAGE_ROOTFS}/usr/share/alsa/ucm2/imx-hdmi-soc
    install -m 0644 ${THISDIR}/files/alsa-ucm/imx-hdmi-soc/HiFi.conf \
        ${IMAGE_ROOTFS}/usr/share/alsa/ucm2/imx-hdmi-soc/HiFi.conf
    install -m 0644 ${THISDIR}/files/alsa-ucm/imx-hdmi-soc/imx-hdmi-soc.conf \
        ${IMAGE_ROOTFS}/usr/share/alsa/ucm2/imx-hdmi-soc/imx-hdmi-soc.conf

    for drv in imx-hdmi-soc imx6qdl-audio-hdmi; do
        install -d ${IMAGE_ROOTFS}/usr/share/alsa/ucm2/conf.d/$drv
        install -m 0644 ${THISDIR}/files/alsa-ucm/imx-hdmi-soc/imx-hdmi-soc.conf \
            ${IMAGE_ROOTFS}/usr/share/alsa/ucm2/conf.d/$drv/imx-hdmi-soc.conf
        install -m 0644 ${THISDIR}/files/alsa-ucm/imx-hdmi-soc/HiFi.conf \
            ${IMAGE_ROOTFS}/usr/share/alsa/ucm2/conf.d/$drv/HiFi.conf
    done

    # Discarded random-password hash keeps key-only root accounts usable without
    # shipping the legacy known password. Network password auth is disabled below.
    sed 's%^root:[^:]*:%root:$6$XtL8jbb03yjkcp8D$2bmg/GWsJQ.L/GQ7L4dX/b5RC.UV./XHRoQHIECZr5lnhiybNDpby7s5hzJmCHmAsrNaIuaaufNSD6Wba3NBW0:%' < ${IMAGE_ROOTFS}/etc/shadow > ${IMAGE_ROOTFS}/etc/shadow.new
    mv ${IMAGE_ROOTFS}/etc/shadow.new ${IMAGE_ROOTFS}/etc/shadow
    chmod 0600 ${IMAGE_ROOTFS}/etc/shadow

    # Private workstation keys are provisioned separately; never put them in a recipe.
    install -d ${IMAGE_ROOTFS}/etc/ssh/sshd_config.d
    printf '%s\n' 'PubkeyAuthentication yes' 'PasswordAuthentication no' 'KbdInteractiveAuthentication no' 'PermitRootLogin prohibit-password' > ${IMAGE_ROOTFS}/etc/ssh/sshd_config.d/00-opensolo.conf

    #pubkey for updater
    mkdir -p ${IMAGE_ROOTFS}/home/root/.ssh
    echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDW+YxPnz9h+qMXWh52H2DwvUwbm4u7LiPcEGp0DtfdEmciCQJDzklNvY0tQgy+xv1C66O4SKUQiNbcWDvW2+5+RsQT2FtzjD9jxnLwZf/O1dK4p8G9sW773/1/z+UnTKDRjuVvuFXcu7a6UjQZ7AYaQZhFRoelJtK5ztmZG7/cv8CYzxBX4EDIY1iah3R3pLNksOVbG+UaOnHPqlHewuAXwkdVzBjb8vuFdXsAaDAD6doECSVhqoaOHysjjrQov+AqKKcMmfZCDbyd6Zl9G8g7q6M7lCNqwUaIA3rK6K3t4pyS0t4oUeiI/mxFjf8S4nLOmWCaYcNCAvWE1uQeniS3" >> ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys

    #syslog is started in rcS (sololink.bb); the rc6.d entry is left as-is
    rm -f ${IMAGE_ROOTFS}/etc/rc[0-5].d/[SK]*syslog
    #this was started as S41 (sololink.bb)
    rm -f ${IMAGE_ROOTFS}/etc/rcS.d/S40networking

    # pick controller's syslog.conf
    rm ${IMAGE_ROOTFS}/etc/syslog.conf.busybox.solo
    mv ${IMAGE_ROOTFS}/etc/syslog.conf.busybox.controller \
       ${IMAGE_ROOTFS}/etc/syslog.conf.busybox
    install -m 0644 ${IMAGE_ROOTFS}/etc/syslog.conf.busybox ${IMAGE_ROOTFS}/etc/syslog.conf
    echo '*.* /log/messages' >> ${IMAGE_ROOTFS}/etc/syslog.conf
    sed -i 's/^ROTATESIZE=.*/ROTATESIZE=2048/; s/^ROTATEGENS=.*/ROTATEGENS=3/' ${IMAGE_ROOTFS}/etc/syslog-startup.conf

    # pick controller's logrotate-sololink.conf
    rm ${IMAGE_ROOTFS}/etc/logrotate-sololink.conf.solo
    mv ${IMAGE_ROOTFS}/etc/logrotate-sololink.conf.controller \
       ${IMAGE_ROOTFS}/etc/logrotate-sololink.conf

    # the private key corresponding to this public key is added to solo root's  ~/.ssh directory in 3dr-solo.bb
    # this is used by dataFlashMAVLink-to-artoo.py (sololink.bb)
    MAV_DF_RSA_PUB_KEY="
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCs1jMmFt8YXua73/trYpRPBmyqABEsSWGlH2qdYEMG6A/8jzgnbV2ECfYUBWQ+Q/qNjkOLV6VpR6WRN/u0bTAdAC+Zwtt3Qxhuby4gXyMPP/6BUkjgCv4ryI9E4QaFzfVHg2wYxhaEIXGfeF4yTqS0M/MpttewQl9ho6ZIe1giaFYCFayX18MBKbeWv88wfiViDvkaANdX/aClY2/YdxDXY+CXXzxcWFqG+8GCOQKfDwYtqcdAnc0DohnBjjf3VpXhNylay91gb23AVSsUaA+6eynufmkdJutbqbNn/uHTq+aidc6bDuLZKYz4ulRYgeqp6aH/7gZPdHZQPHb//Bed root@3dr_solo
"
    echo "$MAV_DF_RSA_PUB_KEY" >>${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
    # Legacy drone/updater identities remain usable only on the flight AP subnet.
    sed -i 's/^ssh-/from="10.1.1.0\/24",restrict ssh-/' ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
    chmod 0700 ${IMAGE_ROOTFS}/home/root/.ssh
    chmod 0600 ${IMAGE_ROOTFS}/home/root/.ssh/authorized_keys
}

ROOTFS_POSTPROCESS_COMMAND += "update_config_files"

IMAGE_FSTYPES = "squashfs sdcard"

export IMAGE_BASENAME = "3dr-controller"
