SUMMARY = "Pixhawk firmware binary"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://${THISDIR}/files/COPYING.txt;md5=d32239bcb673463ab874e80d47fae504"
firmwaredir = "/firmware/green"
FILES:${PN} += "${firmwaredir}/"
REPO_NAME = "ardupilot-solo"
#REPO_TAG = "solo-${PV}"
REPO_TAG = "solo-master"
FILE_EXT = "px4"
FILE_SRC = "ArduCopter-v3_3.5.4_Solo.px4"
FILE_DST = "ArduCopter-v3_3.5.4_Solo.px4"

# Qualified source9 bytes; archive mirror receipt records their origin.
SRC_URI = "https://github.com/Decryptortuning/documentation/raw/PORT/ArduCopter-v3_3.5.4_Solo.px4;downloadfilename=pixhawk-green-3.5.4.px4;unpack=0"
SRC_URI[sha256sum] = "393917d3598299bbf488f8336dfc7cd7dd39e2e34aa76022dd1adaa6b4e37b61"

do_install () {
    install -d ${D}${firmwaredir}
    install -m 0644 ${WORKDIR}/pixhawk-green-3.5.4.px4 ${D}${firmwaredir}/${FILE_DST}
}
