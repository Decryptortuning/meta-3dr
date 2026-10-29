SUMMARY = "Controller HDMI ALSA use case profiles"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://alsa-ucm"
S = "${WORKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "imx_controller"
RDEPENDS:${PN} = "alsa-ucm-conf"
do_install() {
    install -d ${D}${datadir}/alsa/ucm2/imx-hdmi-soc
    install -m 0644 ${WORKDIR}/alsa-ucm/imx-hdmi-soc/*.conf ${D}${datadir}/alsa/ucm2/imx-hdmi-soc/
    for drv in imx-hdmi-soc imx6qdl-audio-hdmi; do
        install -d ${D}${datadir}/alsa/ucm2/conf.d/$drv
        install -m 0644 ${WORKDIR}/alsa-ucm/imx-hdmi-soc/*.conf ${D}${datadir}/alsa/ucm2/conf.d/$drv/
    done
}
FILES:${PN} = "${datadir}/alsa/ucm2"
