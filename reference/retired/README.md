# Retired selections

These machine/distro files are outside BitBake discovery. Only imx_controller,
imx_drone and 3dr-imx-fb are supported. Exact legacy recipe paths are BBMASKed in
conf/layer.conf. The old hostap recipe is not the live resource owner; files/ is.
Artoo 3.0 payloads are historical; 5.0 staged-only artifact and updater exclusion
remain authoritative. libusb1 1.0.20 was an active priority shadow and is replaced
by maintained core 1.0.27; libusb-compat 0.1.5 is retained pending consumer proof.
Disabled 3.10/compat-wireless sources stay disabled; no provider claim is inferred
from file presence. No netfilter/ath6k/UUU removal is included.
