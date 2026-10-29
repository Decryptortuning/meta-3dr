SUMMARY = "sololink-python"
HOMEPAGE = "https://github.com/Decryptortuning/sololink-python"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://LICENSE-APACHE;md5=3b83ef96387f14655fc854ddc3c6bd57"

SRCREV = "${AUTOREV}"
SRC_URI = "git://github.com/Decryptortuning/sololink-python.git;protocol=https;branch=PORT"

PV = "0.0.0"
PKGV = "${PV}+git${GITPKGV}"
S = "${WORKDIR}/git"

inherit setuptools3 gitpkgv
