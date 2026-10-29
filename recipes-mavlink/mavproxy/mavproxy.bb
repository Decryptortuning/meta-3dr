SUMMARY = "This is a MAVLink ground station written in python."
HOMEPAGE = "http://tridge.github.io/MAVProxy/"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://COPYING.txt;md5=3c34afdc3adf82d2448f12715a255122"

PV = "1.4.20-solo"

SRCREV = "ebd19789b45a20346725bcdbae121a3def6f6f4e"
SRC_URI = "git://github.com/Decryptortuning/MAVProxy.git;protocol=https;branch=PORT"

S = "${WORKDIR}/git"

inherit setuptools3

RDEPENDS:${PN} += "${PYTHON_PN}-pyserial \
                   pymavlink \
                   ${PYTHON_PN}-pyparsing \
                  "
