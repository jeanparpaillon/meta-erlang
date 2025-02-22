DESCRIPTION = "SDK type target for building a standalone tarball containing BEAM languages and tools. The \
               tarball can be used to run Erlang and Elixir builds on systems which don't meet the usual version requirements."
SUMMARY = "Standalone tarball for running BEAM builds on systems with does not provide or is hard to get BEAM based languages"
LICENSE = "MIT"

TOOLCHAIN_TARGET_TASK ?= ""

TOOLCHAIN_HOST_TASK ?= "\
    nativesdk-sdk-provides-dummy \
    nativesdk-rebar3 \
    nativesdk-erlfmt \
    nativesdk-elvis \
    nativesdk-erlang \
    nativesdk-erlang-modules-dev \
    nativesdk-elixir \
    nativesdk-elixir-modules-dev \
    "

MULTIMACH_TARGET_SYS = "${SDK_ARCH}-nativesdk${SDK_VENDOR}-${SDK_OS}"
PACKAGE_ARCH = "${SDK_ARCH}_${SDK_OS}"
PACKAGE_ARCHS = ""
TARGET_ARCH = "none"
TARGET_OS = "none"

SDK_PACKAGE_ARCHS += "beamtools-dummy-${SDKPKGSUFFIX}"

require conf/include/beamtools.inc

TOOLCHAIN_OUTPUTNAME ?= "${SDK_ARCH}-beamtools-nativesdk-standalone-${DISTRO_VERSION}-erlang-${ERLANG_VERSION}-elixir-${ELIXIR_VERSION}"

SDK_TITLE = "BEAM tools"
SDK_VERSION = "${@d.getVar('DISTRO_VERSION').replace('snapshot-${METADATA_REVISION}', 'snapshot')}-erlang-${ERLANG_VERSION}-elixir-${ELIXIR_VERSION}"
SDKPATHINSTALL = "/opt/beamtools/${SDK_VERSION}"

RDEPENDS = "${TOOLCHAIN_HOST_TASK}"

EXCLUDE_FROM_WORLD = "1"

inherit populate_sdk
inherit toolchain-scripts-base
inherit nopackages

deltask install
deltask populate_sysroot

do_populate_sdk[stamp-extra-info] = "${PACKAGE_ARCH}"

REAL_MULTIMACH_TARGET_SYS = "none"

create_sdk_files:append () {
	rm -f ${SDK_OUTPUT}/${SDKPATH}/site-config-*
	rm -f ${SDK_OUTPUT}/${SDKPATH}/environment-setup-*
	rm -f ${SDK_OUTPUT}/${SDKPATH}/version-*

	# Generate new (mini) sdk-environment-setup file
	script=${1:-${SDK_OUTPUT}/${SDKPATH}/environment-setup-${SDK_SYS}}
	touch $script
	echo 'export PATH=${SDKPATHNATIVE}${bindir_nativesdk}:${SDKPATHNATIVE}${sbindir_nativesdk}:${SDKPATHNATIVE}${base_bindir_nativesdk}:${SDKPATHNATIVE}${base_sbindir_nativesdk}:$PATH' >> $script
	echo 'export OECORE_NATIVE_SYSROOT="${SDKPATHNATIVE}"' >> $script
	echo 'export GIT_SSL_CAINFO="${SDKPATHNATIVE}${sysconfdir}/ssl/certs/ca-certificates.crt"' >>$script
	echo 'export SSL_CERT_FILE="${SDKPATHNATIVE}${sysconfdir}/ssl/certs/ca-certificates.crt"' >>$script

	toolchain_create_sdk_version ${SDK_OUTPUT}/${SDKPATH}/version-${SDK_SYS}
	beamtools_add_version ${SDK_OUTPUT}/${SDKPATH}/version-${SDK_SYS}

	cat >> $script <<EOF
if [ -d "\$OECORE_NATIVE_SYSROOT/environment-setup.d" ]; then
	for envfile in \$OECORE_NATIVE_SYSROOT/environment-setup.d/*.sh; do
		. \$envfile
	done
fi
# We have to unset this else it can confuse oe-selftest and other tools
# which may also use the overlapping namespace.
unset OECORE_NATIVE_SYSROOT
EOF

	if [ "${SDKMACHINE}" = "i686" ]; then
		echo 'export NO32LIBS="0"' >>$script
		echo 'echo "$BB_ENV_EXTRAWHITE" | grep -q "NO32LIBS"' >>$script
		echo '[ $? != 0 ] && export BB_ENV_EXTRAWHITE="NO32LIBS $BB_ENV_EXTRAWHITE"' >>$script
	fi
}

# beamtools-tarball doesn't need config site
TOOLCHAIN_NEED_CONFIGSITE_CACHE = ""

# The recipe doesn't need any default deps
INHIBIT_DEFAULT_DEPS = "1"

python do_testsdk() {
    import oeqa.sdk.testsdk
    testsdk = oeqa.sdk.testsdk.TestSDK()

    cases_path = os.path.join(os.path.abspath(os.path.dirname(oeqa.sdk.testsdk.__file__)), "beamtools-cases")
    testsdk.context_executor_class.default_cases = cases_path

    testsdk.run(d)
}
addtask testsdk
do_testsdk[nostamp] = "1"
do_testsdk[depends] += "xz-native:do_populate_sysroot"

beamtools_add_version () {
	local versionfile=$1
	echo 'Erlang: ${ERLANG_VERSION}' >> $versionfile
	echo 'Elixir: ${ELIXIR_VERSION}' >> $versionfile
}
