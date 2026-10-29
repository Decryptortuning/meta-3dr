# Package the restriction implementation shipped and maintained with rsync.
PACKAGES =+ "${PN}-rrsync"
RDEPENDS:${PN}-rrsync = "${PN} python3-core python3-shell python3-io python3-fcntl"
FILES:${PN}-rrsync = "${bindir}/rrsync"
do_install:append() {
    install -m 0755 ${S}/support/rrsync ${D}${bindir}/rrsync
    sed -i '1c#!/usr/bin/python3' ${D}${bindir}/rrsync
}
