#!/bin/sh
# Initramfs maintenance functions. Sourcing this file performs no maintenance.
: "${BOOT_STATE_MOUNT:=/mnt/boot-state}"
: "${UPDATE_DIR:=/mnt/log/updates}"
: "${LATEST_MOUNT:=/mnt/latest}"
: "${GOLDEN_MOUNT:=/mnt/golden}"
: "${LATEST_CHECK_MOUNT:=/mnt/latest_check}"
PROVISION_FILE="${BOOT_STATE_MOUNT}/.opensolo-provisioning"

set_machine_payload() {
    case "$SOLO_MACHINE" in
        imx_controller) PAYLOAD_DTB=imx6solo-controller.dtb; PAYLOAD_ROOTFS=3dr-controller-imx_controller.squashfs ;;
        imx_drone) PAYLOAD_DTB=imx6solo-drone.dtb; PAYLOAD_ROOTFS=3dr-solo-imx_drone.squashfs ;;
        *) fatal "Unsupported initramfs machine: $SOLO_MACHINE" ;;
    esac
    PAYLOAD_FILES="uImage $PAYLOAD_DTB $PAYLOAD_ROOTFS u-boot.imx"
}

calculate_partition_layout() {
    P1_END=$(part_end_sector 1)
    P1_SIZE=$(part_size_sectors 1)
    SECTOR_SIZE=$(mmc_sector_size)
    DISK_SECTORS=$(parted -sm -- "$BOOT_DISK" unit s print | awk -F: -v disk="$BOOT_DISK" '$1==disk {gsub("s", "", $2); print $2}')
    for number in "$P1_END" "$P1_SIZE" "$SECTOR_SIZE" "$DISK_SECTORS"; do
        case "$number" in ""|0*|*[!0-9]*) fatal "Invalid disk or GOLDEN geometry" ;; esac
    done
    P2_START=$((P1_END + 1)); P2_END=$((P2_START + P1_SIZE - 1))
    if [ "${PROVISION_MODE:-new}" = settings ]; then
        P2_END=$(part_end_sector 2)
        case "$P2_END" in ""|0*|*[!0-9]*) fatal "Invalid LATEST geometry for settings reset" ;; esac
    fi
    P3_START=$((P2_END + 1))
    P3_END=$((P3_START + (100 * 1024 * 1024 + SECTOR_SIZE - 1) / SECTOR_SIZE - 1))
    P4_START=$((P3_END + 1)); P4_END=$((DISK_SECTORS - 1))
    [ "$P4_END" -gt "$P4_START" ] || fatal "Boot disk cannot hold LATEST, RWFS and LOG"
}

load_provision_state() {
    PROVISION_ACTIVE=0; PROVISION_MODE=new; PROVISION_READY=0
    mkdir -p "$BOOT_STATE_MOUNT" || fatal "Cannot create boot-state mountpoint"
    mount -o ro "$(part_dev 1)" "$BOOT_STATE_MOUNT" || fatal "Cannot read GOLDEN recovery state"
    if [ -e "$PROVISION_FILE" ]; then
        IFS=' ' read -r version PROVISION_MODE state_end state_size PROVISION_READY extra < "$PROVISION_FILE"
        [ "$version" = 1 ] && [ -z "$extra" ] || fatal "Invalid provisioning state version"
        case "$PROVISION_MODE" in new|reset|settings) ;; *) fatal "Invalid provisioning state mode" ;; esac
        case "$PROVISION_READY" in [0-7]) ;; *) fatal "Invalid provisioning progress" ;; esac
        calculate_partition_layout
        [ "$state_end" = "$P1_END" ] && [ "$state_size" = "$P1_SIZE" ] || fatal "Provisioning state does not match GOLDEN"
        [ "$(wc -l < "$PROVISION_FILE")" -eq 1 ] || fatal "Invalid provisioning state length"
        PROVISION_ACTIVE=1
    fi
    umount "$BOOT_STATE_MOUNT" || fatal "Cannot release GOLDEN recovery state"
}

save_provision_state() {
    mkdir -p "$BOOT_STATE_MOUNT" || fatal "Cannot create boot-state mountpoint"
    mount "$(part_dev 1)" "$BOOT_STATE_MOUNT" || fatal "Cannot write GOLDEN recovery state"
    printf '1 %s %s %s %s\n' "$PROVISION_MODE" "$P1_END" "$P1_SIZE" "$PROVISION_READY" > "${PROVISION_FILE}.new" \
        || fatal "Cannot write provisioning progress"
    sync || fatal "Cannot sync provisioning progress"
    mv "${PROVISION_FILE}.new" "$PROVISION_FILE" || fatal "Cannot commit provisioning progress"
    sync || fatal "Cannot sync provisioning commit"
    umount "$BOOT_STATE_MOUNT" || fatal "Cannot release GOLDEN recovery state"
    PROVISION_ACTIVE=1
}

begin_provisioning() {
    PROVISION_MODE="$1"; PROVISION_READY=0
    calculate_partition_layout
    save_provision_state
}

clear_provision_state() {
    mount "$(part_dev 1)" "$BOOT_STATE_MOUNT" || fatal "Cannot finish provisioning state"
    rm -f "$PROVISION_FILE" "${PROVISION_FILE}.new" || fatal "Cannot clear provisioning progress"
    sync || fatal "Cannot sync provisioning completion"
    umount "$BOOT_STATE_MOUNT" || fatal "Cannot release provisioning completion"
    PROVISION_ACTIVE=0
}

partition_exists() { [ -n "$(part_info "$1")" ]; }

all_data_partitions_exist() {
    partition_exists 2 && partition_exists 3 && partition_exists 4
}

ensure_partition() {
    ep_num="$1"; ep_type="$2"; ep_start="$3"; ep_end="$4"
    if ! partition_exists "$ep_num"; then
        parted -s -- "$BOOT_DISK" unit s mkpart primary "$ep_type" "${ep_start}s" "${ep_end}s" \
            || fatal "Failed to create partition $ep_num"
    fi
    wait_for_part "$ep_num" || fatal "Partition $ep_num did not appear"
    ep_actual_start=$(part_info "$ep_num" | awk -F: '{gsub("s", "", $2); print $2}')
    ep_actual_end=$(part_end_sector "$ep_num")
    [ "$ep_actual_start" = "$ep_start" ] && [ "$ep_actual_end" = "$ep_end" ] \
        || fatal "Partition $ep_num does not match recorded provisioning geometry"
}

filesystem_matches() {
    fm_dev=$(part_dev "$1")
    [ "$(blkid -p -s TYPE -o value "$fm_dev" 2>/dev/null)" = "$2" ] \
        && [ "$(blkid -p -s LABEL -o value "$fm_dev" 2>/dev/null)" = "$3" ]
}

ensure_filesystem() {
    ef_num="$1"; ef_type="$2"; ef_label="$3"; ef_bit="$4"
    if [ $((PROVISION_READY & ef_bit)) -ne 0 ]; then
        filesystem_matches "$ef_num" "$ef_type" "$ef_label" || fatal "Recorded filesystem $ef_num is not valid"
        return 0
    fi
    case "$ef_type" in
        vfat) mkfs.vfat "$(part_dev "$ef_num")" -n "$ef_label" || fatal "Failed to format $ef_label partition" ;;
        ext3) mkfs.ext3 -F "$(part_dev "$ef_num")" -L "$ef_label" -q || fatal "Failed to format $ef_label partition" ;;
    esac
    sync || fatal "Cannot sync $ef_label filesystem"
    filesystem_matches "$ef_num" "$ef_type" "$ef_label" || fatal "$ef_label format verification failed"
    PROVISION_READY=$((PROVISION_READY | ef_bit))
    save_provision_state
}

provision_partitions() {
    [ "$PROVISION_ACTIVE" = 1 ] || fatal "Provisioning requires a durable transaction"
    calculate_partition_layout
    if [ "$PROVISION_MODE" = reset ] || [ "$PROVISION_MODE" = settings ]; then
        # Resets intentionally discard data. Repeating them after interruption
        # is allowed only by the recorded reset request on GOLDEN.
        if [ "$PROVISION_MODE" = reset ] && partition_exists 2; then
            parted -s -- "$BOOT_DISK" rm 2 || fatal "Failed to remove LATEST for factory reset"
        fi
        for reset_part in 4 3; do
            if partition_exists "$reset_part"; then
                parted -s -- "$BOOT_DISK" rm "$reset_part" || fatal "Failed to remove partition $reset_part for reset"
            fi
        done
        PROVISION_READY=0
        save_provision_state
    fi
    if [ "$PROVISION_MODE" != settings ]; then
        ensure_partition 2 fat32 "$P2_START" "$P2_END"
    fi
    ensure_partition 3 ext2 "$P3_START" "$P3_END"
    ensure_partition 4 ext2 "$P4_START" "$P4_END"
    if [ "$PROVISION_MODE" != settings ]; then ensure_filesystem 2 vfat LATEST 1; fi
    ensure_filesystem 3 ext3 RWFS 2
    ensure_filesystem 4 ext3 LOG 4
}

update_error() { UPDATE_ERROR="$*"; echo "$UPDATE_ERROR" >&2; return 1; }

select_update_archive() {
    UPDATE_ARCHIVE=
    for candidate in "$UPDATE_DIR"/*.tar.gz; do
        [ -f "$candidate" ] || continue
        [ -z "$UPDATE_ARCHIVE" ] || { update_error "Multiple update archives; keep only the intended archive"; return 1; }
        UPDATE_ARCHIVE="$candidate"
    done
    [ -n "$UPDATE_ARCHIVE" ] || { update_error "No update archive"; return 1; }
}

# BusyBox's minimal od omits -A/-t/-N. hexdump supports this bounded format
# without enabling the global DESKTOP option for every applet.
file_magic() { hexdump -n 4 -e '4/1 "%02x"' "$1"; }

validate_payload() {
    vp_dir="$1"
    for vp_file in $PAYLOAD_FILES; do
        [ -f "$vp_dir/$vp_file" ] && [ ! -L "$vp_dir/$vp_file" ] && [ -s "$vp_dir/$vp_file" ] \
            && [ "$(stat -c %h "$vp_dir/$vp_file")" = 1 ] \
            || { update_error "Missing or invalid payload: $vp_file"; return 1; }
    done
    [ "$(file_magic "$vp_dir/uImage")" = 27051956 ] \
        && [ "$(file_magic "$vp_dir/$PAYLOAD_DTB")" = d00dfeed ] \
        && [ "$(file_magic "$vp_dir/$PAYLOAD_ROOTFS")" = 68737173 ] \
        && [ "$(file_magic "$vp_dir/u-boot.imx")" = d1002040 ] \
        || { update_error "Payload headers do not match expected image formats"; return 1; }
    vp_size=$(stat -c %s "$vp_dir/u-boot.imx") || return 1
    [ "$vp_size" -le 785408 ] || { update_error "Bootloader overlaps saved environment"; return 1; }
}

prepare_update() {
    UPDATE_ERROR=; UPDATE_STAGE="$UPDATE_DIR/.staging"
    [ -f "$UPDATE_ARCHIVE" ] && [ ! -L "$UPDATE_ARCHIVE" ] || { update_error "No update archive"; return 1; }
    update_name=${UPDATE_ARCHIVE##*/}
    case "$update_name" in ""|*[!a-zA-Z0-9._-]*) update_error "Unsupported archive filename"; return 1 ;; esac
    [ -f "${UPDATE_ARCHIVE}.md5" ] && [ ! -L "${UPDATE_ARCHIVE}.md5" ] || { update_error "MD5 sidecar missing"; return 1; }
    [ "$(wc -l < "${UPDATE_ARCHIVE}.md5")" -eq 1 ] || { update_error "Invalid MD5 sidecar"; return 1; }
    IFS=' ' read -r expected_md5 expected_name extra < "${UPDATE_ARCHIVE}.md5"
    case "$expected_md5" in *[!0-9a-fA-F]*|"") update_error "Invalid MD5 value"; return 1 ;; esac
    [ "${#expected_md5}" -eq 32 ] && [ "$expected_name" = "$update_name" ] && [ -z "$extra" ] \
        || { update_error "MD5 sidecar does not name this archive"; return 1; }
    actual_md5=$(md5sum "$UPDATE_ARCHIVE") || { update_error "Cannot checksum archive"; return 1; }
    [ "${actual_md5%% *}" = "$expected_md5" ] || { update_error "MD5 mismatch"; return 1; }
    [ ! -L "$UPDATE_STAGE" ] || { update_error "Invalid staging directory"; return 1; }
    rm -rf "$UPDATE_STAGE" && mkdir -p "$UPDATE_STAGE" || { update_error "Cannot create staging directory"; return 1; }
    printf '%s\n' $PAYLOAD_FILES | sort > "$UPDATE_STAGE/expected"
    tar -tzf "$UPDATE_ARCHIVE" > "$UPDATE_STAGE/members" || { update_error "Unreadable archive"; return 1; }
    sort "$UPDATE_STAGE/members" > "$UPDATE_STAGE/sorted"
    cmp -s "$UPDATE_STAGE/expected" "$UPDATE_STAGE/sorted" || { update_error "Wrong-machine, duplicate or unexpected archive members"; return 1; }
    tar -tvzf "$UPDATE_ARCHIVE" > "$UPDATE_STAGE/details" || { update_error "Cannot inspect archive"; return 1; }
    # GNU and BusyBox tar both start regular-file lines with '-' and put size
    # in column three with six fields. BusyBox hardlinks add a link suffix;
    # reject those as well as symlinks, directories and malformed size fields.
    payload_bytes=$(awk 'NF!=6 || $1 !~ /^-/ || $3 !~ /^[0-9]+$/ || $3==0 {bad=1} {sum+=$3} END {if (bad || NR!=4) exit 1; printf "%.0f", sum}' "$UPDATE_STAGE/details") \
        || { update_error "Archive members must be four regular files"; return 1; }
    available_kib=$(df -Pk "$UPDATE_DIR" | awk 'END {print $4}')
    case "$available_kib" in ""|*[!0-9]*) update_error "Cannot determine staging capacity"; return 1 ;; esac
    [ $((available_kib * 1024)) -ge $((payload_bytes + 4 * 1024 * 1024)) ] \
        || { update_error "Insufficient LOG capacity for update staging"; return 1; }
    latest_sectors=$(part_size_sectors 2); update_sector_size=$(mmc_sector_size)
    case "$latest_sectors:$update_sector_size" in *[!0-9:]*|:*|*:) update_error "Invalid LATEST capacity"; return 1 ;; esac
    [ $((latest_sectors * update_sector_size)) -ge $((payload_bytes + 16 * 1024 * 1024)) ] \
        || { update_error "Update does not fit LATEST; reprovision a larger image"; return 1; }
    mkdir "$UPDATE_STAGE/payload" || { update_error "Cannot create payload staging"; return 1; }
    tar -xzf "$UPDATE_ARCHIVE" -C "$UPDATE_STAGE/payload" || { update_error "Update extraction failed"; return 1; }
    validate_payload "$UPDATE_STAGE/payload" || return 1
    (cd "$UPDATE_STAGE/payload" && sha256sum $PAYLOAD_FILES) > "$UPDATE_STAGE/payload.sha256" \
        || { update_error "Cannot checksum staged payload"; return 1; }
    sync || { update_error "Cannot sync staged update"; return 1; }
}

read_raw_bootloader() {
    rb_size="$1"; rb_dest="$2"
    dd if="$BOOT_DISK" of="${rb_dest}.blocks" bs=512 skip=2 count=$(((rb_size + 511) / 512)) \
        || { update_error "Cannot read raw bootloader"; return 1; }
    head -c "$rb_size" "${rb_dest}.blocks" > "$rb_dest" || { update_error "Cannot trim bootloader readback"; return 1; }
    [ "$(stat -c %s "$rb_dest")" = "$rb_size" ] || { update_error "Short bootloader readback"; return 1; }
    rm -f "${rb_dest}.blocks"
}

flash_bootloader() {
    fb_candidate="$1"; fb_verify="$2"
    fb_size=$(stat -c %s "$fb_candidate") || { update_error "Cannot size bootloader"; return 1; }
    [ "$fb_size" -gt 0 ] && [ "$fb_size" -le 785408 ] || { update_error "Invalid bootloader size"; return 1; }
    read_raw_bootloader "$fb_size" "$fb_verify" || return 1
    if cmp -s "$fb_candidate" "$fb_verify"; then
        echo "Raw bootloader is unchanged"
        return 0
    fi
    dd if="$fb_candidate" of="$BOOT_DISK" bs=512 seek=2 conv=notrunc,fsync \
        || { update_error "Raw bootloader write failed"; return 1; }
    sync || { update_error "Cannot sync bootloader write"; return 1; }
    read_raw_bootloader "$fb_size" "$fb_verify" || return 1
    cmp -s "$fb_candidate" "$fb_verify" || { update_error "Raw bootloader readback mismatch"; return 1; }
}

install_update() {
    LATEST_MOUNTED=0
    prepare_update || return 1
    mkfs.vfat "$(part_dev 2)" -n LATEST || { update_error "LATEST format failed"; return 1; }
    mkdir -p "$LATEST_MOUNT" || { update_error "Cannot create LATEST mountpoint"; return 1; }
    mount "$(part_dev 2)" "$LATEST_MOUNT" || { update_error "LATEST mount failed"; return 1; }
    LATEST_MOUNTED=1
    for copy_file in $PAYLOAD_FILES; do
        cp "$UPDATE_STAGE/payload/$copy_file" "$LATEST_MOUNT/$copy_file" || { update_error "LATEST copy failed: $copy_file"; return 1; }
    done
    (cd "$LATEST_MOUNT" && sha256sum -c "$UPDATE_STAGE/payload.sha256") \
        || { update_error "LATEST payload verification failed"; return 1; }
    sync || { update_error "Cannot sync LATEST update"; return 1; }
    umount "$LATEST_MOUNT" || { update_error "Cannot unmount LATEST update"; return 1; }
    LATEST_MOUNTED=0
    flash_bootloader "$UPDATE_STAGE/payload/u-boot.imx" "$UPDATE_STAGE/bootloader.readback" || return 1
}

finish_update() {
    sync || { update_error "Cannot sync verified update"; return 1; }
    rm -f "$UPDATE_DIR/UPDATEFAILED" || { update_error "Cannot clear previous update failure"; return 1; }
    rm -f "$UPDATE_DIR/UPDATE" || { update_error "Cannot clear completed update request"; return 1; }
    sync || {
        touch "$UPDATE_DIR/UPDATE" || true
        update_error "Cannot sync update completion"; return 1
    }
}

record_update_failure() {
    if [ "${LATEST_MOUNTED:-0}" = 1 ]; then umount "$LATEST_MOUNT" || true; fi
    printf '%s\n' "${UPDATE_ERROR:-Update operation failed}" > "$UPDATE_DIR/UPDATEFAILED" \
        || fatal "Cannot record update failure"
    sync || fatal "Cannot sync update failure"
}
