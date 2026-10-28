FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += " \
    file://volatiles \
    "

SRC_URI += "file://timestamp-format.patch"
