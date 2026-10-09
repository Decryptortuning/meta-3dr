DESCRIPTION = "The /init script for 3dr initramfs"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
SRC_URI = "file://init file://maintenance.sh"
PACKAGE_ARCH = "${MACHINE_ARCH}"

FILES:${PN} = "/"

do_install() {
    install -m 0755 ${WORKDIR}/init ${D}/init
    install -d ${D}${base_libdir}/opensolo ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/maintenance.sh ${D}${base_libdir}/opensolo/maintenance.sh
    printf '%s\n' '${MACHINE}' > ${D}${sysconfdir}/opensolo-machine
}

