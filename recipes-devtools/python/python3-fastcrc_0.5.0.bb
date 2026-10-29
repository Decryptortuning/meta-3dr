SUMMARY = "Native CRC acceleration used by current PyMAVLink"
HOMEPAGE = "https://github.com/overcat/fastcrc"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=9f98c306ca1909411a5a2c70a0e498b8"
PV = "0.5.0"
SRC_URI = "https://files.pythonhosted.org/packages/58/bd/791d8656cd672cc2bc06fda83f5ce369ab5d3b8bf902ee270034d2cae4e8/fastcrc-0.5.0.tar.gz"
SRC_URI[sha256sum] = "e02cdf379d7371f0bd9d7cac957c67f0696e62292d73eebe02ff6393eef05b50"
SRC_URI += "file://0001-fastcrc-omit-build-local-wheel-inventory.patch"
S = "${WORKDIR}/fastcrc-${PV}"

inherit python_maturin
require python3-fastcrc-crates.inc
RDEPENDS:${PN} += "python3-core"
# The native build backend must use the pinned Yocto toolchain, never rustup.
export MATURIN_NO_INSTALL_RUST = "1"
export MATURIN_PEP517_ARGS = "--locked --offline"
