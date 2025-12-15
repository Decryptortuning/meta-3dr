SUMMARY = "stm32loader.py for bootloading the stm32"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://COPYING3;md5=4fe869ee987a340198fb0d54c55c47f1"

SRCREV = "${AUTOREV}"
SRC_URI = "git://github.com/Decryptortuning/stm32loader.git;protocol=https;branch=PORT"

S = "${WORKDIR}/git"

FILES:${PN} += "${bindir}/"

do_install () {
	install -d ${D}${bindir}

	install -m 0755 ${S}/stm32loader.py ${D}${bindir}
}
