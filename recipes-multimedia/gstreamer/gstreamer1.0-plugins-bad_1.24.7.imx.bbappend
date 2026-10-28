# 3dr-solo installs the KMS plugin; NXP enables it by default for mx8.
PACKAGECONFIG:append:imx_drone = " kms"

# Keep this drone-specific plugin set out of shared SoC-architecture packages.
PACKAGE_ARCH:imx_drone = "${MACHINE_ARCH}"
