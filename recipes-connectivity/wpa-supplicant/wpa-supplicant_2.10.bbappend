FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# Use project-specific WPA config/scripts
SRC_URI:append = " file://wpa_supplicant.conf file://wpa-supplicant.sh file://99_wpa_supplicant"

# Permit per-network HT diagnostics on the controller. Default PHY negotiation
# is unchanged until an explicit disable_ht/HT override is configured.
PACKAGE_ARCH:imx-solo = "${MACHINE_ARCH}"

do_configure:append:imx_controller() {
    echo CONFIG_HT_OVERRIDES=y >> ${S}/wpa_supplicant/.config
}

do_install:append:imx-solo() {
    mv ${D}${sysconfdir}/wpa_supplicant.conf ${D}${sysconfdir}/wpa_supplicant.orig
}
FILES:${PN}:append:imx-solo = " ${sysconfdir}/wpa_supplicant.orig"
CONFFILES:${PN}:remove:imx-solo = "${sysconfdir}/wpa_supplicant.conf"
