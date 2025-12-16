SUMMARY = "Boot-time ADV7610 EDID/HPD setup helper"
DESCRIPTION = "Programs a known-good EDID into ADV7610 via the upstream adv7604 V4L2 subdev API so HPD asserts and HDMI locks during boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

SRC_URI = "file://adv7610-edid.c \
           file://adv7610-edid.init \
           "

S = "${WORKDIR}"

inherit update-rc.d

INITSCRIPT_NAME = "adv7610-edid"
INITSCRIPT_PARAMS = "start 35 S ."

do_compile() {
	${CC} ${CFLAGS} ${LDFLAGS} adv7610-edid.c -o adv7610-edid
}

do_install() {
	install -d ${D}${sbindir}
	install -m 0755 ${S}/adv7610-edid ${D}${sbindir}/adv7610-edid

	install -d ${D}${sysconfdir}/init.d
	install -m 0755 ${WORKDIR}/adv7610-edid.init ${D}${sysconfdir}/init.d/adv7610-edid
}

