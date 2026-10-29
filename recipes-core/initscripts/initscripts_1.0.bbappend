FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += " \
    file://volatiles \
    "

SRC_URI += "file://timestamp-format.patch"

do_install:append:imx-solo() {
    # bootmisc consumes this YYYYMMDDhhmmss timestamp; checkpoint service updates it.
    echo '${@time.strftime("%Y%m%d%H%M%S", time.gmtime(int(d.getVar("REPRODUCIBLE_TIMESTAMP_ROOTFS"))))}' > ${D}${sysconfdir}/timestamp
}

PACKAGE_ARCH:imx-solo = "${MACHINE_ARCH}"
