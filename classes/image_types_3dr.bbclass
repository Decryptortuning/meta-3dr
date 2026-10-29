# OpenSolo's existing MBR/GOLDEN/update ABI, deliberately limited to its two boards.
inherit image_types python3native
BOOT_SCRIPTS ?= ""
IMAGE_BOOTLOADER = "u-boot"
GOLDEN_VOLUME_ID = "GOLDEN"
BOOT_SPACE = "90000"
IMAGE_ROOTFS_ALIGNMENT = "4096"
SDCARD = "${IMGDEPLOYDIR}/${IMAGE_NAME}.sdcard"
OPENSOLO_BUILD_ID ?= "${REPRODUCIBLE_TIMESTAMP_ROOTFS}"
OPENSOLO_VOLUME_SERIAL = "${@__import__('hashlib').sha256((d.getVar('OPENSOLO_BUILD_ID') + ':' + d.getVar('MACHINE')).encode()).hexdigest()[:8]}"

do_image_sdcard[depends] += "parted-native:do_populate_sysroot dosfstools-native:do_populate_sysroot mtools-native:do_populate_sysroot python3-native:do_populate_sysroot virtual/kernel:do_deploy u-boot-imx:do_deploy ${PN}:do_image_squashfs"

IMAGE_CMD:sdcard () {
    case '${MACHINE}' in imx_controller|imx_drone) ;; *) bbfatal 'Unsupported OpenSolo machine' ;; esac
    export SOURCE_DATE_EPOCH='${REPRODUCIBLE_TIMESTAMP_ROOTFS}' TZ=UTC LC_ALL=C
    test "$SOURCE_DATE_EPOCH" -ge 315532800 || bbfatal 'Source epoch must be representable in FAT (1980 or later)'
    test "$SOURCE_DATE_EPOCH" -le 4354819198 || bbfatal 'Source epoch exceeds FAT date range'
    test '${KERNEL_IMAGETYPE}' = uImage || bbfatal 'OpenSolo boot ABI requires uImage'
    test -z '${BOOT_SCRIPTS}' || bbfatal 'OpenSolo update ABI does not include arbitrary boot scripts'
    set -- ${KERNEL_DEVICETREE}
    test "$#" -eq 1 || bbfatal 'OpenSolo update ABI requires exactly one DTB'
    DTB_NAME=$(basename "$1")
    KERNEL_BIN='${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-initramfs-${MACHINE}.bin'
    DTB_BIN="${DEPLOY_DIR_IMAGE}/$DTB_NAME"
    UBOOT_BIN='${DEPLOY_DIR_IMAGE}/u-boot-${MACHINE}.imx'
    ROOTFS_IMAGE='${IMGDEPLOYDIR}/${IMAGE_NAME}.squashfs'
    for input in "$KERNEL_BIN" "$DTB_BIN" "$UBOOT_BIN" "$ROOTFS_IMAGE"; do
        test -s "$input" || bbfatal "Missing required current boot payload: $input"
    done
    # Raw bootloader starts at 1KiB and must end before the environment at 0xc0000.
    UBOOT_BYTES=$(stat -Lc%s "$UBOOT_BIN")
    test "$UBOOT_BYTES" -le 785408 || bbfatal 'U-Boot overlaps the raw environment at 0xc0000'
    KERNEL_BYTES=$(stat -Lc%s "$KERNEL_BIN")
    DTB_BYTES=$(stat -Lc%s "$DTB_BIN")
    ROOTFS_BYTES=$(stat -Lc%s "$ROOTFS_IMAGE")
    PAYLOAD_BYTES=$(expr "$KERNEL_BYTES" + "$DTB_BYTES" + "$ROOTFS_BYTES" + "$UBOOT_BYTES")
    BOOT_ROUNDED_BYTES=$(expr "$PAYLOAD_BYTES" + 1023)
    PAYLOAD_KIB=$(expr "$BOOT_ROUNDED_BYTES" / 1024)
    BOOT_KIB=$(expr "$PAYLOAD_KIB" + 16384)
    test "$BOOT_KIB" -ge '${BOOT_SPACE}' || BOOT_KIB='${BOOT_SPACE}'
    GOLDEN_ROUNDED_KIB=$(expr "$BOOT_KIB" + 4095)
    GOLDEN_UNITS=$(expr "$GOLDEN_ROUNDED_KIB" / 4096)
    GOLDEN_KIB=$(expr "$GOLDEN_UNITS" \* 4096)
    END_KIB=$(expr 4096 + "$GOLDEN_KIB")
    # Recreate from zeros, including reserved/environment sectors. No stale bytes.
    rm -f '${SDCARD}' '${WORKDIR}/boot.img'
    IMAGE_KIB=$(expr "$END_KIB" + 1)
    IMAGE_BYTES=$(expr "$IMAGE_KIB" \* 1024)
    truncate -s "$IMAGE_BYTES" '${SDCARD}'
    parted -s '${SDCARD}' mklabel msdos
    parted -s '${SDCARD}' unit KiB mkpart primary fat32 4096 "$END_KIB"
    # parted generates a random MBR signature; use the same stable serial as FAT.
    ${PYTHON} -c 'import struct; p="${SDCARD}"; f=open(p,"r+b"); f.seek(440); f.write(struct.pack("<I",int("${OPENSOLO_VOLUME_SERIAL}",16))); f.close()'
    dd if="$UBOOT_BIN" of='${SDCARD}' conv=notrunc seek=2 bs=512
    mkfs.vfat --invariant -i '${OPENSOLO_VOLUME_SERIAL}' -n '${GOLDEN_VOLUME_ID}' -S 512 -C '${WORKDIR}/boot.img' "$GOLDEN_KIB"
    # mtools 4.0.43 getTimeNow() consumes SOURCE_DATE_EPOCH for file creation,
    # access and modification dates when -m is absent. Never preserve host mtime.
    mcopy -i '${WORKDIR}/boot.img' "$DTB_BIN" "::/$DTB_NAME"
    mcopy -i '${WORKDIR}/boot.img' "$ROOTFS_IMAGE" '::/${IMAGE_BASENAME}-${MACHINE}.squashfs'
    mcopy -i '${WORKDIR}/boot.img' "$UBOOT_BIN" '::/u-boot.imx'
    mcopy -i '${WORKDIR}/boot.img' "$KERNEL_BIN" '::/uImage'
    dd if='${WORKDIR}/boot.img' of='${SDCARD}' conv=notrunc seek=1 bs=4194304
    PAYLOAD_DIR='${WORKDIR}/opensolo-update'
    rm -rf "$PAYLOAD_DIR"
    install -d "$PAYLOAD_DIR"
    install -m 0644 "$DTB_BIN" "$PAYLOAD_DIR/$DTB_NAME"
    install -m 0644 "$ROOTFS_IMAGE" "$PAYLOAD_DIR/${IMAGE_BASENAME}-${MACHINE}.squashfs"
    install -m 0644 "$UBOOT_BIN" "$PAYLOAD_DIR/u-boot.imx"
    install -m 0644 "$KERNEL_BIN" "$PAYLOAD_DIR/uImage"
    # Exactly four legacy members; receipts stay outside the wire archive.
    tar --sort=name --format=gnu --owner=0 --group=0 --numeric-owner --mtime="@$SOURCE_DATE_EPOCH" -C "$PAYLOAD_DIR" -cf '${WORKDIR}/opensolo-update.tar' '${IMAGE_BASENAME}-${MACHINE}.squashfs' "$DTB_NAME" u-boot.imx uImage
    gzip -n -c '${WORKDIR}/opensolo-update.tar' > '${IMGDEPLOYDIR}/${IMAGE_BASENAME}.tar.gz'
    (cd '${IMGDEPLOYDIR}' && md5sum '${IMAGE_BASENAME}.tar.gz' > '${IMAGE_BASENAME}.tar.gz.md5')
    # Machine-readable selection receipt, independent of temporary/deploy symlinks.
    ${PYTHON} -c 'import hashlib,json,pathlib; p=pathlib.Path("${WORKDIR}/opensolo-update"); h=lambda x:hashlib.sha256(x.read_bytes()).hexdigest(); out=pathlib.Path("${IMGDEPLOYDIR}"); data=dict(format_version=1,machine="${MACHINE}",build_id="${OPENSOLO_BUILD_ID}",source_date_epoch=int("${REPRODUCIBLE_TIMESTAMP_ROOTFS}"),fat_volume_serial="${OPENSOLO_VOLUME_SERIAL}",partition_start_bytes=4194304,bootloader_offset_bytes=1024,environment_offset_bytes=786432,payloads={x.name:h(x) for x in sorted(p.iterdir())},sdcard_sha256=h(pathlib.Path("${SDCARD}")),archive_sha256=h(out/"${IMAGE_BASENAME}.tar.gz")); (out/"${IMAGE_BASENAME}.boot-manifest.json").write_text(json.dumps(data,sort_keys=True,indent=2)+"\n")'
}
