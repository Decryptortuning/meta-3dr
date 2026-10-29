SUMMARY = "OpenSolo controller runtime"
LICENSE = "MIT"
# packagegroup snapshots the architecture while inheriting; set it first.
PACKAGE_ARCH = "${MACHINE_ARCH}"
inherit packagegroup
RDEPENDS:${PN} = "fsl-rc-local imx-gst1.0-plugin gstreamer1.0-plugins-base gstreamer1.0-plugins-base-app gstreamer1.0-plugins-good gstreamer1.0-plugins-good-udp gstreamer1.0-plugins-good-rtp gstreamer1.0-plugins-bad-mpegtsdemux gstreamer1.0-plugins-bad-videoparsersbad gstreamer1.0-libav gstreamer1.0 v4l-utils libudev python3-core python3-modules python3-numpy python3-posix-ipc alsa-ucm-conf openssh iptables iw wireless-tools hostapd dnsmasq sololink sololink-python util-linux artoo-firmware e2fsprogs-e2fsck dosfstools openssh-sftp-server 3dr-splash persist-logs rsync stm32loader opensolo-access opensolo-log-receiver opensolo-platform-config opensolo-controller-ucm"
RDEPENDS:${PN}:append = " ${@d.getVar('OPENSOLO_DEVELOPMENT_PACKAGES') if d.getVar('OPENSOLO_BUILD_PROFILE') == 'development' else ''}"
