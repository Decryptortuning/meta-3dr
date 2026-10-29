FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append:imx-solo = " file://opensolo-console"
do_install:append:imx-solo() {
    install -m 0755 ${WORKDIR}/opensolo-console ${D}${sysconfdir}/init.d/rc_mxc.S
}
