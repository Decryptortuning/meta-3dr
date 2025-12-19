require recipes-core/images/core-image-minimal.bb

IMAGE_INSTALL += "3dr-initscript parted mtd-utils mtd-utils-ubifs e2fsprogs-mke2fs util-linux dosfstools e2fsprogs-e2fsck firmware-imx-sdma-imx6q"

IMAGE_FSTYPES = "${INITRAMFS_FSTYPES}"

export IMAGE_BASENAME = "3dr-initramfs"

# The initramfs runs as PID 1 before /devtmpfs is mounted by userland; ensure
# /dev/console exists so the kernel can attach stdio for /init.
USE_DEVFS = "0"
IMAGE_DEVICE_TABLES = "files/device_table-3dr-initramfs.txt"
