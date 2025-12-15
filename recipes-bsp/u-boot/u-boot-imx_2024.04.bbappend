# Preserve variant-specific deploy naming if multiple configs are built.
do_deploy:append() {
    if [ -n "${UBOOT_CONFIG}" ]; then
        unset i j
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]; then
                    install -m 644 \
                        ${B}/${config}/${UBOOT_BINARYNAME}-${type}.${UBOOT_SUFFIX} \
                        ${DEPLOYDIR}/${UBOOT_IMAGE}_${type}
                fi
            done
            unset j
        done
        unset i
    fi
}
