LICENSE = "CLOSED"

ALLOW_EMPTY:${PN} = "1"

RDEPENDS:${PN} = " \
    busybox \
    initscripts \
    logrotate \
    sysvinit \
    wtmp-rotate \
    "
