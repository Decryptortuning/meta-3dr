FILESEXTRAPATHS:prepend := "${THISDIR}/hostap-daemon-2.4:"

# Replace the generic hostapd init script/config with the Solo/Artoo versions.
SRC_URI:append = " file://hostapd.conf"

do_install:append() {
    install -m 0644 ${WORKDIR}/hostapd.conf ${D}${sysconfdir}/hostapd.conf
}
