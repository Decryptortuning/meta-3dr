do_install:append:imx_controller() {
    sed -i '/case "$1" in/i\if [ "$1" = start ] && [ -f /etc/sololink.conf ] && ! grep -iq "^ApEnable[[:space:]]*=[[:space:]]*True[[:space:]]*$" /etc/sololink.conf; then echo "AP DHCP disabled by profile"; exit 0; fi' ${D}${sysconfdir}/init.d/dnsmasq
}
