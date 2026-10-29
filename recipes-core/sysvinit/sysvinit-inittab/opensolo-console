#!/bin/sh
# SysV init owns this session. Preserve its PID/session with exec; UART1 is
# exclusively the MCU bridge, regardless of inherited console guessing.
exec /sbin/getty -L ttymxc0 115200 vt100
