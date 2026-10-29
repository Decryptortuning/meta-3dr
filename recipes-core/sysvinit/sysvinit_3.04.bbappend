do_install:append:imx-solo() {
    sed -i 's/^HWCLOCKACCESS=.*/HWCLOCKACCESS=no/; s#^PSPLASH_FIFO_DIR=.*#PSPLASH_FIFO_DIR=/psplash#' ${D}${sysconfdir}/default/rcS
    grep -q '^HWCLOCKACCESS=' ${D}${sysconfdir}/default/rcS || echo HWCLOCKACCESS=no >> ${D}${sysconfdir}/default/rcS
}

PACKAGE_ARCH:imx-solo = "${MACHINE_ARCH}"
