SUMMARY = "Current PyMAVLink runtime with the explicit OpenSolo wire dialect"
HOMEPAGE = "https://github.com/ArduPilot/pymavlink"
LICENSE = "LGPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6ea13ec5f0f3dd35ac5b53afdc3ed9ff"
PV = "2.4.50"
SRC_URI = "https://files.pythonhosted.org/packages/3c/4e/212af84fdb81b62e934faf1ae579cc5ffbb32d2756e9c7c63feff9a62877/pymavlink-2.4.50.tar.gz"
SRC_URI[sha256sum] = "8d65a8b05b2971babeef4b6ca74a247a72276f62f7559466c64e96ece7f10062"
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://0001-opensolo-modern-runtime-contract.patch file://solo-dialect file://generate-solo.py"
S = "${WORKDIR}/pymavlink-${PV}"

inherit setuptools3

DEPENDS += "python3-cython-native python3-lxml-native"
RDEPENDS:${PN} += "python3-modules python3-pyserial python3-lxml python3-fastcrc"

# The optional DataFlash indexer is unrelated to the control/parser contract.
# Keep published standard dialects and generate the distinct product dialect.
export PYMAVLINK_FAST_INDEX = "0"
export NOGEN = "1"
export PYTHONHASHSEED = "0"

do_compile:prepend() {
    export SOURCE_DATE_EPOCH="${REPRODUCIBLE_TIMESTAMP_ROOTFS}"
    ${PYTHON} ${WORKDIR}/generate-solo.py ${S} ${WORKDIR}/solo-dialect ${B}/solo-headers
}

do_install:append() {
    install -d ${D}${includedir}/opensolo/mavlink ${D}${datadir}/opensolo
    cp -R ${B}/solo-headers/. ${D}${includedir}/opensolo/mavlink/
    install -m 0644 ${B}/solo-contract.json ${D}${datadir}/opensolo/mavlink-contract.json
}

FILES:${PN} += "${datadir}/opensolo/mavlink-contract.json"
FILES:${PN}-dev += "${includedir}/opensolo/mavlink"
