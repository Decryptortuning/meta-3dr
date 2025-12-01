LICENSE = "CLOSED"

ALLOW_EMPTY:${PN} = "1"

DEPENDS_${PN} = " \
    busybox \
    initscripts \
    logrotate \
    sysvinit \
    wtmp-rotate \
    "

RDEPENDS:${PN} = " \
    busybox \
    initscripts \
    logrotate \
    sysvinit \
    wtmp-rotate \
    "
