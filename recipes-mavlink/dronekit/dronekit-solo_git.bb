SUMMARY = "dronekit-solo"
HOMEPAGE = "https://github.com/Decryptortuning/dronekit-python-solo"

LICENSE = "GPLv3"
LIC_FILES_CHKSUM = "file://requirements.txt;md5=b6106de10adcc12b60d3cf95b9017b7f"
PV = "1.2.0"

SRCREV = "1.2.0"
SRC_URI = "https://github.com/Decryptortuning/dronekit-python-solo.git;branch=PORT"

S = "${WORKDIR}/git"

inherit setuptools3

RDEPENDS:${PN} += "${PYTHON_PN}-pyserial \
                   pymavlink \
                   ${PYTHON_PN}-pyparsing \
                   dronekit \
                  "
