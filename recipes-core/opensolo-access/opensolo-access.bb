SUMMARY = "OpenSolo key-only access and explicitly paired log transfer"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://10-opensolo-access.conf file://sololog.conf file://opensolo-log-receiver file://opensolo-log-sender file://README.md"
S = "${WORKDIR}"
inherit allarch useradd update-rc.d

PACKAGES =+ "opensolo-log-receiver opensolo-log-sender"
USERADD_PACKAGES = "opensolo-log-receiver"
# '*' cannot authenticate as a password, but is not Linux's locked '!' prefix.
# A valid shell is needed for sshd to dispatch the mandatory forced command.
USERADD_PARAM:opensolo-log-receiver = "--system --home /var/lib/sololog --no-create-home --shell /bin/sh --password '*' --user-group sololog"

INITSCRIPT_PACKAGES = "opensolo-log-receiver opensolo-log-sender"
INITSCRIPT_NAME:opensolo-log-receiver = "opensolo-log-receiver"
INITSCRIPT_PARAMS:opensolo-log-receiver = "start 18 2 3 4 5 ."
INITSCRIPT_NAME:opensolo-log-sender = "opensolo-log-sender"
INITSCRIPT_PARAMS:opensolo-log-sender = "defaults 95 05"

RDEPENDS:${PN} = "openssh-sshd"
RDEPENDS:opensolo-log-receiver = "opensolo-access rsync-rrsync"
RDEPENDS:opensolo-log-sender = "opensolo-access sololink openssh-ssh rsync"

# Keep root key authentication usable without a shipped password or the Linux
# locked-account prefix. This package owns access policy in both build profiles.
pkg_postinst:${PN}() {
    if [ -n "$D" ]; then
        sed -i 's|^root:[^:]*:|root:*:|' "$D/etc/shadow"
    else
        usermod -p '*' root
    fi
}
RDEPENDS:${PN}:append = " shadow"

do_install() {
    install -d ${D}${sysconfdir}/ssh/sshd_config.d ${D}${sysconfdir}/ssh/sshd_config.tail
    install -d ${D}${sysconfdir}/opensolo/log-receiver ${D}${sysconfdir}/init.d
    install -d -m 0755 ${D}${localstatedir}/lib/sololog
    install -m 0644 ${WORKDIR}/10-opensolo-access.conf ${D}${sysconfdir}/ssh/sshd_config.d/
    install -m 0644 ${WORKDIR}/sololog.conf ${D}${sysconfdir}/ssh/sshd_config.tail/
    install -m 0755 ${WORKDIR}/opensolo-log-receiver ${D}${sysconfdir}/init.d/
    install -m 0755 ${WORKDIR}/opensolo-log-sender ${D}${sysconfdir}/init.d/
    install -d ${D}${docdir}/opensolo-access
    install -m 0644 ${WORKDIR}/README.md ${D}${docdir}/opensolo-access/
}
FILES:opensolo-log-receiver = "${sysconfdir}/ssh/sshd_config.tail/sololog.conf ${sysconfdir}/opensolo/log-receiver ${sysconfdir}/init.d/opensolo-log-receiver ${localstatedir}/lib/sololog"
FILES:opensolo-log-sender = "${sysconfdir}/init.d/opensolo-log-sender"
FILES:${PN} = "${sysconfdir}/ssh/sshd_config.d/10-opensolo-access.conf"
CONFFILES:${PN} = "${sysconfdir}/ssh/sshd_config.d/10-opensolo-access.conf"
CONFFILES:opensolo-log-receiver = "${sysconfdir}/ssh/sshd_config.tail/sololog.conf"
