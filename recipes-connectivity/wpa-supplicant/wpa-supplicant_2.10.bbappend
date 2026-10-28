FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# Use project-specific WPA config/scripts
SRC_URI:append = " file://wpa_supplicant.conf file://wpa-supplicant.sh file://99_wpa_supplicant"

# Permit per-network HT diagnostics on the controller. Default PHY negotiation
# is unchanged until an explicit disable_ht/HT override is configured.
do_configure:append:imx_controller() {
    echo CONFIG_HT_OVERRIDES=y >> ${S}/wpa_supplicant/.config
}
