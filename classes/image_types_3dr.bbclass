inherit image_types

IMAGE_BOOTLOADER ?= "u-boot"

# Handle u-boot suffixes
UBOOT_SUFFIX ?= "bin"
UBOOT_PADDING ?= "0"
UBOOT_SUFFIX_SDCARD ?= "${UBOOT_SUFFIX}"

#
# Handles i.MX mxs bootstream generation
#

# IMX Bootlets Linux bootstream
IMAGE_DEPENDS_linux.sb = "elftosb-native imx-bootlets virtual/kernel"
IMAGE_LINK_NAME_linux.sb = ""
IMAGE_CMD:linux.sb () {
	kernel_bin="`readlink ${KERNEL_IMAGETYPE}-${MACHINE}.bin`"
	kernel_dtb="`readlink ${KERNEL_IMAGETYPE}-${MACHINE}.dtb || true`"
	linux_bd_file=imx-bootlets-linux.bd-${MACHINE}
	if [ `basename $kernel_bin .bin` = `basename $kernel_dtb .dtb` ]; then
		# When using device tree we build a zImage with the dtb
		# appended on the end of the image
		linux_bd_file=imx-bootlets-linux.bd-dtb-${MACHINE}
		cat $kernel_bin $kernel_dtb \
		    > $kernel_bin-dtb
		rm -f ${KERNEL_IMAGETYPE}-${MACHINE}.bin-dtb
		ln -s $kernel_bin-dtb ${KERNEL_IMAGETYPE}-${MACHINE}.bin-dtb
	fi

	# Ensure the file is generated
	rm -f ${IMAGE_NAME}.linux.sb
	elftosb -z -c $linux_bd_file -o ${IMAGE_NAME}.linux.sb

	# Remove the appended file as it is only used here
	rm -f $kernel_bin-dtb
}

# IMX Bootlets barebox bootstream
IMAGE_DEPENDS_barebox.mxsboot-sdcard = "elftosb-native u-boot-mxsboot-native imx-bootlets barebox"
IMAGE_CMD:barebox.mxsboot-sdcard () {
	barebox_bd_file=imx-bootlets-barebox_ivt.bd-${MACHINE}

	# Ensure the files are generated
	rm -f ${IMAGE_NAME}.barebox.sb ${IMAGE_NAME}.barebox.mxsboot-sdcard
	elftosb -f mx28 -z -c $barebox_bd_file -o ${IMAGE_NAME}.barebox.sb
	mxsboot sd ${IMAGE_NAME}.barebox.sb ${IMAGE_NAME}.barebox.mxsboot-sdcard
}

# U-Boot mxsboot generation to SD-Card
UBOOT_SUFFIX_SDCARD_mxs ?= "mxsboot-sdcard"
IMAGE_DEPENDS_uboot.mxsboot-sdcard = "u-boot-mxsboot-native u-boot"
IMAGE_CMD:uboot.mxsboot-sdcard = "mxsboot sd ${DEPLOY_DIR_IMAGE}/u-boot-${MACHINE}.${UBOOT_SUFFIX} \
                                             ${DEPLOY_DIR_IMAGE}/${IMAGE_NAME}.rootfs.uboot.mxsboot-sdcard"

# Boot partition volume id
GOLDEN_VOLUME_ID ?= "GOLDEN"

# Boot partition size [in KiB]
BOOT_SPACE = "90000"

# Barebox environment size [in KiB]
BAREBOX_ENV_SPACE ?= "512"

# Set alignment to 4MB [in KiB]
IMAGE_ROOTFS_ALIGNMENT = "4096"

do_image_sdcard[depends] += "parted-native:do_populate_sysroot \
                             dosfstools-native:do_populate_sysroot \
                             mtools-native:do_populate_sysroot \
                             virtual/kernel:do_deploy \
                             ${@'%s:do_deploy' % d.getVar('IMAGE_BOOTLOADER') if d.getVar('IMAGE_BOOTLOADER') else ''}"

SDCARD = "${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.sdcard"

# The 3DR sdcard generator always copies the squashfs rootfs onto the boot
# partition; keep a non-empty default to satisfy IMAGE_CMD:sdcard checks.
SDCARD_ROOTFS ?= "${IMAGE_LINK_NAME}.squashfs"

# meta-freescale/meta-imx (Scarthgap) uses extended machine override tokens
# like "mx6-generic-bsp" instead of the older "mx6".
SDCARD_GENERATION_COMMAND:mx6-generic-bsp = "generate_imx_sdcard"
SDCARD_GENERATION_COMMAND:mx6dl-generic-bsp = "generate_imx_sdcard"

#
# Create an image that can by written onto a SD card using dd for use
# with i.MX SoC family
#
# External variables needed:
#   ${SDCARD_ROOTFS}    - the rootfs image to incorporate
#   ${IMAGE_BOOTLOADER} - bootloader to use {u-boot, barebox}
#
# The disk layout used is:
#
#    0                      -> IMAGE_ROOTFS_ALIGNMENT         - reserved to bootloader (not partitioned)
#    IMAGE_ROOTFS_ALIGNMENT -> BOOT_SPACE                     - kernel, dtb, squashfs
#            4MiB              96MiB      
# <-----------------------> <------------> 
#  ------------------------ ------------ ------------------------ -------------------------------
# | IMAGE_ROOTFS_ALIGNMENT | GOLDEN_SPACE |                    EMPTY                            |
#  ------------------------ ------------ ------------------------ -------------------------------
# ^                        ^              ^ 
# |                        |              |
# 0                      4096         4MiB + 96MiB
generate_imx_sdcard () {
	# Create partition table
	parted -s ${SDCARD} mklabel msdos
	parted -s ${SDCARD} unit KiB mkpart primary fat32 ${IMAGE_ROOTFS_ALIGNMENT} $(expr ${IMAGE_ROOTFS_ALIGNMENT} \+ ${GOLDEN_SPACE_ALIGNED})
	parted ${SDCARD} print

	# Burn bootloader
	case "${IMAGE_BOOTLOADER}" in
		imx-bootlets)
		bberror "The imx-bootlets is not supported for i.MX based machines"
		exit 1
		;;
		u-boot)
		dd if=${DEPLOY_DIR_IMAGE}/u-boot-${MACHINE}.${UBOOT_SUFFIX_SDCARD} of=${SDCARD} conv=notrunc seek=2 skip=${UBOOT_PADDING} bs=512
		;;
		barebox)
		dd if=${DEPLOY_DIR_IMAGE}/barebox-${MACHINE}.bin of=${SDCARD} conv=notrunc seek=1 skip=1 bs=512
		dd if=${DEPLOY_DIR_IMAGE}/bareboxenv-${MACHINE}.bin of=${SDCARD} conv=notrunc seek=1 bs=512k
		;;
		"")
		;;
		*)
		bberror "Unkown IMAGE_BOOTLOADER value"
		exit 1
		;;
	esac

	# Create golden partition image
	GOLDEN_BLOCKS=$(LC_ALL=C parted -s ${SDCARD} unit b print \
	                  | awk '/ 1 / { print substr($4, 1, length($4 -1)) / 1024 }')
	rm -f ${WORKDIR}/boot.img
	mkfs.vfat -n "${GOLDEN_VOLUME_ID}" -S 512 -C ${WORKDIR}/boot.img $GOLDEN_BLOCKS
	mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE}.bin ::/${KERNEL_IMAGETYPE}
	
	# Copy boot scripts
	for item in ${BOOT_SCRIPTS}; do
		src=`echo $item | awk -F':' '{ print $1 }'`
		dst=`echo $item | awk -F':' '{ print $2 }'`

		mcopy -i ${WORKDIR}/boot.img -s $src ::/$dst
	done

	# Copy device tree file
	if test -n "${KERNEL_DEVICETREE}"; then
		for DTS_FILE in ${KERNEL_DEVICETREE}; do
			DTS_BASE_NAME=`basename ${DTS_FILE} | awk -F "." '{print $1}'`
			if [ -e "${DEPLOY_DIR_IMAGE}/${DTS_BASE_NAME}.dtb" ]; then
				mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${DTS_BASE_NAME}.dtb ::/${DTS_BASE_NAME}.dtb
			fi
		done
	fi

    #Copy the squashfs 
    if [ -e ${DEPLOY_DIR_IMAGE}/${SDCARD_ROOTFS} ]; then
        mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/${SDCARD_ROOTFS} ::/${IMAGE_BASENAME}-${MACHINE}.squashfs
    else
        bberror "Error, no squashfs file ${SDCARD_ROOTFS} found"
        exit 1
    fi
    
    #Copy the u-boot.imx
    if [ -e ${DEPLOY_DIR_IMAGE}/u-boot.imx ]; then
        mcopy -i ${WORKDIR}/boot.img -s ${DEPLOY_DIR_IMAGE}/u-boot.imx ::/u-boot.imx
    else
        bberror "Error, no u-boot.imx file found"
        exit 1
    fi

	# Burn Partition
	dd if=${WORKDIR}/boot.img of=${SDCARD} conv=notrunc seek=1 bs=$(expr ${IMAGE_ROOTFS_ALIGNMENT} \* 1024) && sync && sync

    # Create the update tarball
    cp ${DEPLOY_DIR_IMAGE}/${DTS_BASE_NAME}.dtb ${WORKDIR}/${DTS_BASE_NAME}.dtb
    cp ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE}.bin ${WORKDIR}/${KERNEL_IMAGETYPE}
    cp ${DEPLOY_DIR_IMAGE}/${SDCARD_ROOTFS} ${WORKDIR}/${IMAGE_BASENAME}-${MACHINE}.squashfs
    cp ${DEPLOY_DIR_IMAGE}/u-boot-${MACHINE}.${UBOOT_SUFFIX_SDCARD} ${WORKDIR}/u-boot.imx

    cd ${WORKDIR}
    tar -pczf ${DEPLOY_DIR_IMAGE}/${IMAGE_BASENAME}.tar.gz \
              ${DTS_BASE_NAME}.dtb \
              ${KERNEL_IMAGETYPE} \
              ${IMAGE_BASENAME}-${MACHINE}.squashfs \
              u-boot.imx

    #tarball md5sum
    cd ${DEPLOY_DIR_IMAGE}
    md5sum ${IMAGE_BASENAME}.tar.gz > ${IMAGE_BASENAME}.tar.gz.md5
    cd ${WORKDIR}
}

IMAGE_CMD:sdcard () {
	if [ -z "${SDCARD_ROOTFS}" ]; then
		bberror "SDCARD_ROOTFS is undefined. To use sdcard image from Freescale's BSP it needs to be defined."
		exit 1
	fi

	# Ensure the boot partition is large enough for kernel+dtb+rootfs (+overhead).
	BOOT_SPACE_KIB="${BOOT_SPACE}"
	OVERHEAD_KIB=16384
	ROOTFS_BYTES=$(stat -Lc%s "${DEPLOY_DIR_IMAGE}/${SDCARD_ROOTFS}")
	ROOTFS_KIB=$(expr ${ROOTFS_BYTES} + 1023)
	ROOTFS_KIB=$(expr ${ROOTFS_KIB} / 1024)
	KERNEL_BYTES=$(stat -Lc%s "${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${MACHINE}.bin")
	KERNEL_KIB=$(expr ${KERNEL_BYTES} + 1023)
	KERNEL_KIB=$(expr ${KERNEL_KIB} / 1024)
	UBOOT_BYTES=$(stat -Lc%s "${DEPLOY_DIR_IMAGE}/u-boot-${MACHINE}.${UBOOT_SUFFIX_SDCARD}")
	UBOOT_KIB=$(expr ${UBOOT_BYTES} + 1023)
	UBOOT_KIB=$(expr ${UBOOT_KIB} / 1024)
	DTB_KIB=0
	if test -n "${KERNEL_DEVICETREE}"; then
		for DTS_FILE in ${KERNEL_DEVICETREE}; do
			DTS_BASE_NAME=`basename ${DTS_FILE} | awk -F "." '{print $1}'`
			if [ -e "${DEPLOY_DIR_IMAGE}/${DTS_BASE_NAME}.dtb" ]; then
				DTB_BYTES=$(stat -Lc%s "${DEPLOY_DIR_IMAGE}/${DTS_BASE_NAME}.dtb")
				THIS_DTB_KIB=$(expr ${DTB_BYTES} + 1023)
				THIS_DTB_KIB=$(expr ${THIS_DTB_KIB} / 1024)
				DTB_KIB=$(expr ${DTB_KIB} + ${THIS_DTB_KIB})
			fi
		done
	fi
	NEEDED_BOOT_KIB=$(expr ${ROOTFS_KIB} + ${KERNEL_KIB} + ${UBOOT_KIB} + ${DTB_KIB} + ${OVERHEAD_KIB})
	if [ "${NEEDED_BOOT_KIB}" -gt "${BOOT_SPACE_KIB}" ]; then
		BOOT_SPACE_KIB="${NEEDED_BOOT_KIB}"
	fi

	# Align boot partition and calculate total SD card image size
	GOLDEN_SPACE_ALIGNED=$(expr ${BOOT_SPACE_KIB} + ${IMAGE_ROOTFS_ALIGNMENT} - 1)
	GOLDEN_SPACE_ALIGNED=$(expr ${GOLDEN_SPACE_ALIGNED} - ${GOLDEN_SPACE_ALIGNED} % ${IMAGE_ROOTFS_ALIGNMENT})
	SDCARD_SIZE=$(expr ${IMAGE_ROOTFS_ALIGNMENT} + ${GOLDEN_SPACE_ALIGNED} + 1)

	# Initialize a sparse file
	dd if=/dev/zero of=${SDCARD} bs=1 count=0 seek=$(expr 1024 \* ${SDCARD_SIZE})

	${SDCARD_GENERATION_COMMAND}
}
