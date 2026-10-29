# Retain Scarthgap packaging/configure policy; select the reviewed upstream source.
require recipes-devtools/rsync/rsync_3.2.7.bb

SRC_URI = "https://download.samba.org/pub/rsync/src/rsync-${PV}.tar.gz file://rsyncd.conf file://makefile-no-rebuild-3.5.1.patch"
SRC_URI[sha256sum] = "c55f9c9dc10fb8bec397b399a0fdded53cc9a2d8e30891bb0d63724d25c37bef"
# Optional new IDN feature is unnecessary for pinned numeric controller address.
EXTRA_OECONF:append = " --disable-idn"
