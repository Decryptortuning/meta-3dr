SUMMARY = "OpenSolo platform policy"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://opensolo-mount.ignorelist file://opensolo-sysctl.conf file://opensolo-platform"
S = "${WORKDIR}"
inherit update-rc.d
INITSCRIPT_NAME = "opensolo-platform"
INITSCRIPT_PARAMS = "start 39 S ."
PACKAGE_ARCH = "${MACHINE_ARCH}"
RDEPENDS:${PN} = "python3-core"
do_install() {
    install -d ${D}${sysconfdir}/udev/mount.ignorelist.d ${D}${sysconfdir}/sysctl.d ${D}${bindir} ${D}${sysconfdir}/init.d
    install -m 0644 ${WORKDIR}/opensolo-mount.ignorelist ${D}${sysconfdir}/udev/mount.ignorelist.d/opensolo
    install -m 0644 ${WORKDIR}/opensolo-sysctl.conf ${D}${sysconfdir}/sysctl.d/90-opensolo.conf
    install -m 0755 ${WORKDIR}/opensolo-platform ${D}${sysconfdir}/init.d/opensolo-platform
    ln -s python3 ${D}${bindir}/python
}
FILES:${PN} = "${sysconfdir}/init.d/opensolo-platform ${sysconfdir}/udev/mount.ignorelist.d/opensolo ${sysconfdir}/sysctl.d/90-opensolo.conf ${bindir}/python"
