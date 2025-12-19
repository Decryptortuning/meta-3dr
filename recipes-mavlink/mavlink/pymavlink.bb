SUMMARY = "This is a python implementation of the MAVLink protocol. "
HOMEPAGE = "http://qgroundcontrol.org/mavlink/"

LICENSE = "LGPLv3"
LIC_FILES_CHKSUM = "file://README.txt;md5=2fc3900b33c4131645987a81bfe6a55f"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRCREV = "${AUTOREV}"
SRC_URI = "git://github.com/Decryptortuning/mavlink-solo.git;protocol=https;branch=PORT"
SRC_URI += "file://0001-mavcrc-python3-frombytes.patch"
SRC_URI += "file://0002-disable-mavnative-extension.patch"

PV = "1.1.56"
PKGV = "${PV}+git${GITPKGV}"
S = "${WORKDIR}/git/pymavlink"

inherit setuptools3 gitpkgv

# The upstream setup.py expects these folders to exist when it copies dialect
# XML files during the build.
do_compile:prepend() {
    install -d ${S}/dialects/v09 ${S}/dialects/v10
}

# avoid "error: option --single-version-externally-managed not recognized"
DISTUTILS_INSTALL_ARGS = "--root=${D} \
    --prefix=${prefix} \
    --install-lib=${PYTHON_SITEPACKAGES_DIR} \
    --install-data=${datadir}"
