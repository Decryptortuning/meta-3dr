# make install can regenerate GIR scanners after relinking the target libraries.
# Core exports these build-tree paths only in do_compile; tasks have separate
# environments, so the install scanner needs the same paths explicitly.
do_install:prepend() {
    export GIR_EXTRA_LIBS_PATH="${B}/avahi-gobject/.libs:${B}/avahi-common/.libs:${B}/avahi-client/.libs:${B}/avahi-glib/.libs"
}
