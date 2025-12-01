FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
# Bump PRINC while tolerating it being unset in newer poky
PRINC := "${@int(d.getVar('PRINC') or 0) + 2}"

SRC_URI:append = " file://interfaces-controller \
		   file://interfaces-solo"

do_install:append() {
	install -m 0644 ${WORKDIR}/interfaces-controller ${D}${sysconfdir}/network/
	install -m 0644 ${WORKDIR}/interfaces-solo ${D}${sysconfdir}/network/
}
