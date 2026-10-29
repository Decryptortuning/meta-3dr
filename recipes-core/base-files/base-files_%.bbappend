PACKAGE_ARCH = "${MACHINE_ARCH}"
hostname:imx_controller = "3dr_controller"
hostname:imx_drone = "3dr_solo"
do_install:append:imx-solo() {
    install -d ${D}/log
    echo '/dev/mmcblk0p4 /log auto defaults 0 2' >> ${D}${sysconfdir}/fstab
}
FILES:${PN}:append:imx-solo = " /log"
