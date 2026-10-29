# Validate product policy for every parsed recipe, before tasks can execute.
python () {
    if d.getVar('OPENSOLO_BUILD_PROFILE') not in ('development', 'release'):
        bb.fatal('OPENSOLO_BUILD_PROFILE must be development or release')
}
