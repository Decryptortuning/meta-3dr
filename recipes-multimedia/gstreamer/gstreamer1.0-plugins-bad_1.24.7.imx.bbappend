# 3dr-solo installs the KMS plugin; NXP enables it by default for mx8.
PACKAGECONFIG:append:imx_drone = " kms"

# Use NXP dynamic package-architecture selection for this drone variant.
MACHINE_ARCH_FILTER:append:imx_drone = " gstreamer1.0-plugins-bad"
