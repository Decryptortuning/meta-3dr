SUMMARY = "OpenSolo network time with explicit flight-state ownership"
HOMEPAGE = "https://chrony-project.org/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=751419260aa954499f7abaabaa882bbe"
PV = "4.9"
SRC_URI = "https://chrony-project.org/releases/chrony-4.9.tar.gz"
SRC_URI[sha256sum] = "4924c6f530105bcd5b9e9e33c48a2ae1bfd889222c8480bc41601110efc864d0"
SRC_URI += "file://chrony.conf file://opensolo-time.sources file://chronyd"
S = "${WORKDIR}/chrony-${PV}"
inherit pkgconfig update-rc.d useradd
DEPENDS = "libcap"
USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "--system chronyd"
USERADD_PARAM:${PN} = "--system -d / -M --gid chronyd --shell /bin/nologin chronyd"
INITSCRIPT_NAME = "chronyd"
INITSCRIPT_PARAMS = "start 49 S 3 5 . stop 01 0 1 4 6 ."
RCONFLICTS:${PN} = "ntp ntimed"
RDEPENDS:${PN} = "dpkg-start-stop busybox"
PACKAGES =+ "chronyc"
FILES:chronyc = "${bindir}/chronyc"
CONFFILES:${PN} = "${sysconfdir}/chrony.conf ${sysconfdir}/chrony/sources.d/opensolo.sources"
DISABLE_STATIC = ""
do_configure() {
    ${S}/configure --prefix=${prefix} --sysconfdir=${sysconfdir} \
        --bindir=${bindir} --sbindir=${sbindir} --host-system=Linux \
        --with-user=chronyd --with-pidfile=/run/chrony/chronyd.pid \
        --chronyrundir=/run/chrony --with-ntp-era=${REPRODUCIBLE_TIMESTAMP_ROOTFS} \
        --disable-readline --without-editline --without-seccomp \
        --disable-nts --disable-sechash --without-gnutls --without-nettle \
        --without-nss --without-tomcrypt --disable-refclock --disable-rtc
}
do_compile() {
    oe_runmake
}
do_install() {
    install -d ${D}${bindir} ${D}${sbindir} ${D}${sysconfdir}/init.d ${D}${sysconfdir}/chrony/sources.d
    install -m 0755 ${S}/chronyd ${D}${sbindir}/chronyd
    install -m 0755 ${S}/chronyc ${D}${bindir}/chronyc
    install -m 0644 ${WORKDIR}/chrony.conf ${D}${sysconfdir}/chrony.conf
    install -m 0644 ${WORKDIR}/opensolo-time.sources ${D}${sysconfdir}/chrony/sources.d/opensolo.sources
    install -m 0755 ${WORKDIR}/chronyd ${D}${sysconfdir}/init.d/chronyd
}
