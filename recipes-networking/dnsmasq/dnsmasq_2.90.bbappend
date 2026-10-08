# Preserve the upstream init/config files; only add controller AP policy.
FILESEXTRAPATHS:prepend:imx_controller := "${THISDIR}/files/imx_controller:"
SRC_URI:append:imx_controller = " file://opensolo-controller.conf"
PACKAGE_ARCH:imx_controller = "${MACHINE_ARCH}"
CONFFILES:${PN}:append:imx_controller = " ${sysconfdir}/dnsmasq.d/opensolo-controller.conf"

do_install:append:imx_controller() {
    install -d ${D}${sysconfdir}/dnsmasq.d
    install -m 0644 ${WORKDIR}/opensolo-controller.conf \
        ${D}${sysconfdir}/dnsmasq.d/opensolo-controller.conf
}
