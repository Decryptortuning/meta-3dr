# Preserve the selected i.MX cryptodev engine/backend build policy on core 3.5.
# NXP-only TLS1.2 e_devcrypto algorithm offload extensions and old openssl.cnf
# patch are deferred until source/API + hardware compatibility are established.
# Normal provider-backed TLS and generic cryptodev remain available; no automatic
# provider/engine activation or hardware acceleration claim is made.
PACKAGECONFIG:append:imx-solo = " cryptodev-linux"
EXTRA_OECONF:append:imx-solo = " enable-ktls"
