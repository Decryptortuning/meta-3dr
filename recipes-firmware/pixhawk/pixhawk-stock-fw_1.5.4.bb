SUMMARY = "Pixhawk stock cube firmware binary"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://${THISDIR}/files/COPYING.txt;md5=d32239bcb673463ab874e80d47fae504"

firmwaredir = "/firmware/3dr"
FILES:${PN} += "${firmwaredir}/"

REPO_NAME = "ardupilot-solo"
REPO_TAG = "solo-${PV}"
FILE_EXT = "px4"
FILE_SRC = "ArduCopter-v2.${FILE_EXT}"
FILE_DST = "ArduCopter-StockCube-${PV}.${FILE_EXT}"

# Qualified source9 bytes; archive mirror receipt records their origin.
SRC_URI = "https://github.com/OpenSolo/ardupilot-solo/releases/download/solo-1.5.4/ArduCopter-v2.px4;downloadfilename=pixhawk-stock-1.5.4.px4;unpack=0"
SRC_URI[sha256sum] = "433b49840975413825a817dc8335e0c3852a93e821b389cbf07d1ad0e241b13c"

do_install () {
    install -d ${D}${firmwaredir}
    install -m 0644 ${WORKDIR}/pixhawk-stock-1.5.4.px4 ${D}${firmwaredir}/${FILE_DST}
}
