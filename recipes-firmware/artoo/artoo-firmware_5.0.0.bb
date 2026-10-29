SUMMARY = "Locally qualified OpenSolo 5 Artoo candidate (not auto-programmed)"
LICENSE = "Apache-2.0 & MIT"
# Source/license notices audited at artifact source 60a2707; legacy font aliases
# are byte-identical to Apache-licensed Roboto-Regular.otf in that revision.
FILESEXTRAPATHS:prepend := "${THISDIR}/files/:"
SRC_URI = "file://stick-cfg-evt-mode1.cfg file://stick-cfg-evt-mode2.cfg file://stick-cfg-evt-default.cfg file://artoo-candidate.bin file://artoo-candidate.json"
ARTOO_CANDIDATE_VERSION ?= "unset"
ARTOO_CANDIDATE_SHA256 ?= "unset"
FILES:${PN} += "/firmware"
python do_verify_candidate() {
    import hashlib, json
    from pathlib import Path
    root = Path(d.getVar("WORKDIR"))
    data = (root / "artoo-candidate.bin").read_bytes()
    info = json.loads((root / "artoo-candidate.json").read_text())
    if hashlib.sha256(data).hexdigest() != d.getVar("ARTOO_CANDIDATE_SHA256") or info["sha256"] != d.getVar("ARTOO_CANDIDATE_SHA256"):
        bb.fatal("Artoo candidate hash differs from the integration lock")
    if info["version"] != d.getVar("ARTOO_CANDIDATE_VERSION") or not info.get("staged_only") or len(data) >= 0x3f800:
        bb.fatal("Invalid Artoo candidate identity/size/staging policy")
}
addtask verify_candidate after do_unpack before do_install

do_install() {
    # Boot updater only searches /firmware/artoo_*.bin. Activation is separate.
    install -d ${D}/firmware/candidates ${D}/firmware/cfg
    install -m 0644 ${WORKDIR}/artoo-candidate.bin ${D}/firmware/candidates/artoo_${ARTOO_CANDIDATE_VERSION}.bin
    install -m 0644 ${WORKDIR}/artoo-candidate.json ${D}/firmware/candidates/manifest.json
    install -m 0644 ${WORKDIR}/stick-cfg-evt-*.cfg ${D}/firmware/cfg/
}

SRC_URI += "file://LICENSE-APACHE.candidate file://LICENSE-MIT-ringbuffer.candidate"
LIC_FILES_CHKSUM = "file://${WORKDIR}/LICENSE-APACHE.candidate;md5=3b83ef96387f14655fc854ddc3c6bd57 file://${WORKDIR}/LICENSE-MIT-ringbuffer.candidate;md5=4af81d195917b22b0b4267414899d11e"

do_install:append() {
    install -d ${D}${datadir}/licenses/${PN}
    install -m 0644 ${WORKDIR}/LICENSE-*.candidate ${D}${datadir}/licenses/${PN}/
}
FILES:${PN} += "${datadir}/licenses/${PN}"
