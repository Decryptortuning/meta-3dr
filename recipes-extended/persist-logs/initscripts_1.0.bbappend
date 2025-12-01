FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += " \
    file://logrotate-dmesg.conf \
    file://dmesg.sh-patch \
    "

do_install:prepend() {
    patch -p 1 ${WORKDIR}/dmesg.sh < ${WORKDIR}/dmesg.sh-patch
}

do_install:append() {
    install -m 0644 ${WORKDIR}/logrotate-dmesg.conf ${D}${sysconfdir}
}
