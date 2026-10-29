# Match blocks must be last: a Match in sshd_config.d would make subsequent
# global directives (e.g. UsePAM) invalid. The receiver alone owns this tail.
do_install:append() {
    for config in sshd_config sshd_config_readonly; do
        echo 'Include /etc/ssh/sshd_config.tail/*.conf' >> ${D}${sysconfdir}/ssh/$config
    done
}
