include recipes-core/images/core-image-base.bb
inherit image_types_3dr
PV = "3.0.0"
OPENSOLO_BUILD_ID ?= "${REPRODUCIBLE_TIMESTAMP_ROOTFS}"
VER_NAME = "OpenSolo 5 ${OPENSOLO_BUILD_PROFILE}"
IMAGE_FEATURES:remove = "debug-tweaks allow-empty-password empty-root-password"
IMAGE_FEATURES += "package-management ssh-server-openssh"
IMAGE_FSTYPES = "squashfs sdcard"
do_rootfs[depends] += "virtual/kernel:do_bundle_initramfs"
do_image_sdcard[depends] += "parted-native:do_populate_sysroot dosfstools-native:do_populate_sysroot mtools-native:do_populate_sysroot virtual/kernel:do_deploy"
# Image identity is the only product rootfs postprocess operation. The selected
# epoch is supplied by the locked integration configuration, never wall time.
opensolo_image_identity() {
    printf '%s\n' '${PV}' '${IMAGE_BASENAME}-${MACHINE}-${OPENSOLO_BUILD_ID}' '${VER_NAME}' 'Build Date: ${@time.strftime("%Y%m%d%H%M%S", time.gmtime(int(d.getVar("REPRODUCIBLE_TIMESTAMP_ROOTFS"))))}' > ${IMAGE_ROOTFS}/VERSION
}
ROOTFS_POSTPROCESS_COMMAND += "opensolo_image_identity; "
