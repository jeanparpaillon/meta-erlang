include erlang.inc
include erlang-${PV}.inc
require erlang-${PV}-manifest.inc

PR = "r0"

FILESEXTRAPATHS_prepend := "${THISDIR}/files/18:"

OTP_BUILD_CONFIGURE_OPTS = "skip"

SRC_URI += "file://erlang-fix-build-issue-in-Yocto.patch"
