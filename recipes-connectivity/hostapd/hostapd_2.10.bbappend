FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Replace the generic hostapd init script/config with the Solo/Artoo versions.
SRC_URI:append = " file://hostapd.conf"

do_install:append() {
    install -m 0644 ${WORKDIR}/hostapd.conf ${D}${sysconfdir}/hostapd.conf
}

do_install:append:imx-solo() {
    mv ${D}${sysconfdir}/hostapd.conf ${D}${sysconfdir}/hostapd.orig
}
FILES:${PN}:append:imx-solo = " ${sysconfdir}/hostapd.orig"
CONFFILES:${PN}:remove:imx-solo = "${sysconfdir}/hostapd.conf"

PACKAGE_ARCH:imx-solo = "${MACHINE_ARCH}"

# Startup validates private provisioned configuration before launching AP.
RDEPENDS:${PN}:append:imx-solo = " sololink-config"
