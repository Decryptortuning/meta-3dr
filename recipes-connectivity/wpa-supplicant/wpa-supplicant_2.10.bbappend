FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# Use project-specific WPA config/scripts
SRC_URI:append = " file://wpa_supplicant.conf file://wpa-supplicant.sh file://99_wpa_supplicant"
