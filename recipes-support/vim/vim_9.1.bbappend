SUMMARY = "Vi IMproved - enhanced vi editor"

do_install:append() {
    # Avoid picking up perl tools as hard deps during packaging.
    chmod -x ${D}${datadir}/${BPN}/${VIMDIR}/tools/*.pl || true
}
