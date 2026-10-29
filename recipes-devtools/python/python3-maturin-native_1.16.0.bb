SUMMARY = "Current native Maturin PEP517 backend for Rust Python extensions"
HOMEPAGE = "https://github.com/PyO3/maturin"
LICENSE = "MIT | Apache-2.0"
LIC_FILES_CHKSUM = "file://license-apache;md5=1836efb2eb779966696f473ee8540542 file://license-mit;md5=85fd3b67069cff784d98ebfc7d5c0797"
PV = "1.16.0"
SRC_URI = "https://files.pythonhosted.org/packages/bd/71/eb078174335ff30bde1be2ad54fb6256db4b160eb3efa81163fa30f8c814/maturin-1.16.0.tar.gz"
SRC_URI[sha256sum] = "13e69ca12542999d8c5467131de72a49bb76e4c284b9a45c5783df7c0fcb3a8a"
S = "${WORKDIR}/maturin-${PV}"

inherit cargo native python3-dir
require python3-maturin-crates.inc
DEPENDS += "python3-native openssl-native pkgconfig-native"

# Bootstrap the native executable directly; building its own wheel would add a
# setuptools-rust/backend cycle. The upstream pure-Python backend calls this binary.
do_install() {
    install -d ${D}${bindir} ${D}${PYTHON_SITEPACKAGES_DIR}
    install -m 0755 ${B}/target/${CARGO_TARGET_SUBDIR}/maturin ${D}${bindir}/maturin
    cp -R ${S}/maturin ${D}${PYTHON_SITEPACKAGES_DIR}/
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}/maturin-${PV}.dist-info
    install -m 0644 ${S}/PKG-INFO ${D}${PYTHON_SITEPACKAGES_DIR}/maturin-${PV}.dist-info/METADATA
}
