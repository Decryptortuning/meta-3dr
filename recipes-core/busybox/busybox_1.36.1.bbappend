# Maintenance needs checked, flushed writes without truncating regular-file
# test images. Keep the upstream defconfig and add only this feature fragment.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"


SRC_URI:append:imx-solo = " file://maintenance-dd.cfg file://syslog-controller.conf file://syslog-solo.conf file://syslog-startup.conf"
PACKAGE_ARCH:imx-solo = "${MACHINE_ARCH}"
OPENSOLO_ROLE:imx_controller = "controller"
OPENSOLO_ROLE:imx_drone = "solo"
INITSCRIPT_PARAMS:${PN}-syslog:imx-solo = "start 40 S . stop 20 6 ."
do_install:append:imx-solo() {
    install -m 0644 ${WORKDIR}/syslog-${OPENSOLO_ROLE}.conf ${D}${sysconfdir}/syslog.conf
    install -m 0644 ${WORKDIR}/syslog-startup.conf ${D}${sysconfdir}/syslog-startup.conf
}
