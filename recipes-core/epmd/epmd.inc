DESCRIPTION = "Erlang Port Mapper Daemon."
HOMEPAGE = "https://erlang.org/doc/man/epmd.html"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=ff253ad767462c46be284da12dda33e8"
SECTION = "network"
PR = "r0"

SRC_URI = "git://github.com/erlang/otp;branch=master;protocol=https \
           file://epmd.init \
           file://epmd.service \
           file://epmd.socket \
           file://erlang-fix-build-issue-in-Yocto.patch \
           file://fix-wx-configure.patch \
          "

S = "${WORKDIR}/git"

DEPENDS = "erlang-native ncurses zlib"

PARALLEL_MAKE = ""

inherit autotools-brokensep

PACKAGECONFIG ??= "${@bb.utils.filter('DISTRO_FEATURES', 'systemd', d)}"
PACKAGECONFIG[systemd] = "--enable-systemd,,systemd"

TARGET_CC_ARCH += "${LDFLAGS}"

EXTRA_OEMAKE = "BUILD_CC='${BUILD_CC}'"

NATIVE_BIN = "${STAGING_LIBDIR_NATIVE}/erlang/bin"

CACHED_CONFIGUREVARS += "ac_cv_prog_javac_ver_1_2=no ac_cv_prog_javac_ver_1_5=no erl_xcomp_sysroot=${STAGING_DIR_TARGET}"

do_configure() {
    cd ${S}; ./otp_build autoconf; cd -
    cd ${S}/erts; autoreconf; cd -

    . ${CONFIG_SITE}

    SHLIB_LD='${CC}' \
    oe_runconf

    sed -i -e 's|$(ERL_TOP)/bin/dialyzer|${NATIVE_BIN}/dialyzer --output_plt $@ -pa $(ERL_TOP)/lib/kernel/ebin -pa $(ERL_TOP)/lib/stdlib/ebin|' lib/dialyzer/src/Makefile
}

do_compile() {
    cd ${S}/erts/epmd
    PATH=${NATIVE_BIN}:$PATH \    
    oe_runmake ERL_TOP=${S} TARGET=${TARGET_SYS}
}

do_install() {
    if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
        install -d ${D}${systemd_unitdir}/system
        install -m 0644 ${WORKDIR}/epmd.service ${D}${systemd_system_unitdir}
        install -m 0644 ${WORKDIR}/epmd.socket ${D}${systemd_system_unitdir}
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        install -d ${D}${sysconfdir}/init.d
        install -m 0755 ${WORKDIR}/epmd.init ${D}${sysconfdir}/init.d/epmd
    fi

    install -d ${D}/${sbindir}
    install -m 0755 ${S}/bin/${TARGET_SYS}/epmd ${D}/${sbindir}/epmd
}

inherit useradd update-rc.d systemd

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "--system epmd"
USERADD_PARAM:${PN}  = "--system --create-home --home /var/lib/epmd \
                        -g epmd epmd"

INITSCRIPT_NAME = "epmd"
INITSCRIPT_PARAMS = "defaults"

SYSTEMD_SERVICE:${PN} = "epmd.service epmd.socket"

FILES:${PN} += "${systemd_system_unitdir}"
