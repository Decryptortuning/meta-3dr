SUMMARY = "dronekit"
HOMEPAGE = "https://github.com/Decryptortuning/dronekit-python"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d2794c0df5b907fdace235a619d80314"

SRCREV = "9b1fa7631df5e564dc4d47a42d5b3e8003b44d5e"
SRC_URI = "git://github.com/Decryptortuning/dronekit-python.git;protocol=https;branch=PORT"

PV = "2.4.0"
S = "${WORKDIR}/git"

inherit setuptools3

RDEPENDS:${PN} += "${PYTHON_PN}-pyserial \
                   pymavlink \
                   ${PYTHON_PN}-pyparsing \
                  "
