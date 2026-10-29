SUMMARY = "OpenSolo solo runtime"
LICENSE = "MIT"
# packagegroup snapshots the architecture while inheriting; set it first.
PACKAGE_ARCH = "${MACHINE_ARCH}"
inherit packagegroup
RDEPENDS:${PN} = "chrony chronyc fsl-rc-local imx-gst1.0-plugin gstreamer1.0-plugins-base gstreamer1.0-plugins-base-app gstreamer1.0-plugins-good gstreamer1.0-plugins-good-udp gstreamer1.0-plugins-good-rtp gstreamer1.0-plugins-bad-mpegtsmux gstreamer1.0-plugins-bad-videoparsersbad gstreamer1.0-plugins-bad-kms v4l-utils adv7610-edid libudev python3-core python3-modules python3-numpy python3-posix-ipc openssh iptables iw wireless-tools hostapd dnsmasq sololink sololink-python shotmanager rpm util-linux pixhawk-stock-fw pixhawk-green-fw gimbal-firmware e2fsprogs-e2fsck dosfstools openssh-sftp-server persist-logs rsync opensolo-access opensolo-platform-config"
RDEPENDS:${PN}:append = " ${@d.getVar('OPENSOLO_DEVELOPMENT_PACKAGES') if d.getVar('OPENSOLO_BUILD_PROFILE') == 'development' else ''}"
