# Maintenance needs checked, flushed writes without truncating regular-file
# test images. Keep the upstream defconfig and add only this feature fragment.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI:append:imx-solo = " file://maintenance-dd.cfg"
