# OpenSolo access and log pairing contract

Both `OPENSOLO_BUILD_PROFILE=development` and `release` use key-only SSH. Images
contain no device private identity, log-transfer authorization or trusted host
key. No SSH connection or host-key scan is performed by provisioning. The
operator explicitly selects the two trusted device filesystems and installs
public credentials offline. Never pair a generic image before cloning it.

Package ownership:

* `opensolo-access`: global key-only SSH policy; root's non-authenticating `*`
  password marker (not Linux's `!` account-lock prefix). Operator public-key
  login stays available once provisioned. Console password login is disabled.
* `opensolo-log-receiver`: controller-only `sololog` account, root-owned
  `/var/lib/sololog` home, forced receiver, init directory ownership.
* `sololink`: installs the Python producer in `/usr/bin` and provision helper
  in `/usr/sbin`. The drone inittab `DSYN:4:respawn` entry is the sole producer
  lifecycle owner, alongside `DFL:4:respawn` for the dataflash logger. No second
  init service starts the producer.
* `rsync-rrsync`: exact upstream `support/rrsync` from the selected rsync source.
  No custom remote-command whitelist. `openssh` append installs a tail include
  after its global directives so the receiver Match cannot corrupt their scope.

## Fresh-media cutover gate

This source wave produces local images for fresh/spare media with a **fresh
RWFS overlay**. Before hardware acceptance, explicitly provision each device's
own identity, the peer trust relationship and an operator public key. Private
operator keys remain outside the image and are never copied by this workflow.

A retained RWFS overlay can shadow the new `/etc/ssh/sshd_config`, bypass its
includes, retain old root authorized_keys and retain formerly shared secrets.
Installing the new rootfs alone does **not** revoke that retained state. Reusing
an existing overlay requires inspection and reconciliation under a separately
authorized coordinated cutover before declaring the access policy effective.
No boot migration, automatic key deletion or forced overlay wipe is performed.
Fresh-rootfs validation does not establish the policy of an existing device.

## Offline provisioning

Use distinct, mounted writable **effective device root filesystems**, including
each device's persistent overlay, not a raw GOLDEN squashfs or a shared staging
image. `DRONE` and `CONTROLLER` below are absolute paths to these mounted roots.
Mounting and flashing are separate operator actions. Execute the helper from the
checkout on the Linux workstation (requires Python 3 and OpenSSH ssh-keygen):

```sh
sudo python3 flightcode/dflog/opensolo-log-provision identity --role drone --root "$DRONE"
sudo python3 flightcode/dflog/opensolo-log-provision identity --role controller --root "$CONTROLLER"
sudo python3 flightcode/dflog/opensolo-log-provision pair --drone-root "$DRONE" --controller-root "$CONTROLLER"
sudo python3 flightcode/dflog/opensolo-log-provision authorize-admin --root "$DRONE" --public-key /path/to/operator_ed25519.pub
sudo python3 flightcode/dflog/opensolo-log-provision authorize-admin --root "$CONTROLLER" --public-key /path/to/operator_ed25519.pub
```

`identity` preserves existing private identities and verifies the public key
matches. Controller identity is its standard `/etc/ssh/ssh_host_ed25519_key`;
OpenSSH retains it on boot. `pair` requires both identities to exist and match
their public files; it never generates/accepts a remote key opportunistically.
Only the drone public key is written to the controller's root-owned
`/etc/opensolo/log-receiver/authorized_keys` (0644, directory 0755). Only the
controller public host key is written to the drone's `known_hosts` (0600).
The private drone identity remains in `/etc/opensolo/log-transfer` (0700) and
is 0600. The final `pair.json` write enables transfer. `authorize-admin` replaces
the root authorized_keys file with the selected operator Ed25519 public key;
it never reads or copies the operator private key. These commands require root.
Re-pair explicitly after resetting/replacing a device identity. Interrupted
pairing can leave transfer unavailable, never substitute another trusted host.

In runlevel 4, an unpaired drone still maintains `/log/RECENT*.BIN` links. It reports
`controller log transfer unavailable` to syslog and retries every ten seconds;
it never reports disabled transfer as successful. Pairing becomes effective on
the next iteration. Source logs are bounded to the most recent 50 numeric
regular `.BIN` files. Symlink inputs are excluded. A temporary same-filesystem
hardlink snapshot adds ten RECENT names as **regular files**, preserving app
read semantics without exposing symlinks to the receiver. Crash-left private
snapshot directories are removed on sender startup. Active logs may grow while
transferring; later iterations resume/update them (`--partial`, `--size-only`).

The sender authenticates as `sololog@10.1.1.1:.`, with an explicit identity,
`StrictHostKeyChecking=yes`, a dedicated known_hosts file, no global trust file,
no password prompts and no automatic host-key updates. The dot is relative to
rrsync's restricted root `/log/solo/dataflash`. `--delete` intentionally prunes
only this mirror. Numeric logs and RECENT files arrive readable as 0644 under
0755 directories; existing root/app readers retain access at the original path.
Owned source search found no controller writer/reader requiring root ownership
of these files; external apps consume the preserved paths. Controller tlog uses
`/log/solo.tlog`, outside this tree. No recursive ownership changes affect it.

`sololog` has an unusable password `*` and a valid `/bin/sh` only so sshd can
invoke its forced `rrsync -wo -munge /log/solo/dataflash` command. Key options
also apply `restrict`; Match disables forwarding, PTY, tunnel and user rc.
The account cannot replace its root-owned authorization/home or parent log
folders. Upstream rrsync rejects pull, traversal and unsafe option requests;
`-munge` makes uploaded symlinks unusable. This is restricted rsync, not a
chroot: keep rsync security fixes current. The source-confirmed Linux OpenSSH
9.6p1 `configure.ac` defines locked prefix `!`; `platform_locked_account()` does
not reject `*` as a locked account. PAM account expiry still applies normally.
The root workstation account has no forced command and keeps independent keys.

## Validation gate and scope

Do not execute until all coordinated source work is frozen and root review gives
GO. Extract the exact selected rsync archive's `support/rrsync` to a temporary
file and set `RRSYNC_SOURCE` to its absolute path, then run:

```sh
RRSYNC_SOURCE=/absolute/path/to/rrsync python3 -m unittest discover -s flightcode/dflog/tests -p test_secure_transfer.py -v
```

Tests generate disposable keys and use a local transport shim, passing the real
producer rsync arguments into the exact upstream receiver. They cover partial
recovery, deletion scope, numeric source symlinks, RECENT readability, receiver
symlink munging, malicious commands/options, identity pinning and provisioning
mismatch/symlink rejection. They require host rsync and ssh-keygen; no network,
SSH daemon, device or hardware is used. Integration validation must additionally
inspect built rootfs package ownership and run target `sshd -T -C` for root and
sololog to verify the effective key-only policy/forced-command split. Verify LOG
mount startup, directory ownership and actual paired-device transfer during the
separately authorized hardware acceptance stage. Source/offline checks do not
establish target OpenSSH/PAM runtime or Wi-Fi acceptance.
