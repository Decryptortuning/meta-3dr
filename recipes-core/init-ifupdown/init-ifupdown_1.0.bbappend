FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI:append = " file://interfaces-controller file://interfaces-solo"
OPENSOLO_ROLE:imx_controller = "controller"
OPENSOLO_ROLE:imx_drone = "solo"
INITSCRIPT_PACKAGES:imx-solo = ""
do_install:append:imx-solo() {
    install -m 0644 ${WORKDIR}/interfaces-${OPENSOLO_ROLE} ${D}${sysconfdir}/network/interfaces
    # The SoloLink configuration package owns the custom networking service.
    rm -f ${D}${sysconfdir}/init.d/networking
}
