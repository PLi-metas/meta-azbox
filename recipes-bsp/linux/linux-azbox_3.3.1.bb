SUMMARY = "Linux kernel for ${MACHINE}"
SECTION = "kernel"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://${WORKDIR}/linux-${KV}/COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "azboxhd"
DEPENDS = "genromfs-native virtual/${TARGET_PREFIX}gcc"

inherit kernel machine_kernel_pr

PKG_kernel-base = "kernel-base"
PKG_kernel-image = "kernel-image"
RPROVIDES_kernel-base = "kernel-${KERNEL_VERSION}"
RPROVIDES_kernel-image = "kernel-image-${KERNEL_VERSION}"
ALLOW_EMPTY_kernel-dev = "1"

KV = "3.3.1"
SRC = "2015"
SRCREV = "r4"
SRCDATE = "02022014"

SRC_URI += "http://source.mynonpublic.com/linux-azbox-${KV}-new-2.tar.bz2;name=azbox-kernel \
    http://source.mynonpublic.com/${MACHINE}/${MACHINE}-${KV}-${SRC}-${SRCREV}.tar.gz;name=azbox-kernel-${MACHINE} \
    file://mips-refactor-clearpage-and-copypage.patch \
    file://defconfig \
    file://genzbf.c \
    file://sigblock.h \
    file://zboot.h \
    file://emhwlib_registers_tango2.h \
    file://sata.patch \
    file://fixme-hardfloat.patch \
    file://rtl8712-fix-warnings.patch \
    file://rtl8187se-fix-warnings.patch \
    file://0001-kernel-add-support-for-gcc-5.patch \
    file://kernel-add-support-for-gcc6.patch \
    file://kernel-add-support-for-gcc7.patch \
    file://kernel-add-support-for-gcc8.patch \
    file://dvb_frontend-Multistream-support-3.3.patch \
    file://timeconst_perl5.patch \
    file://genksyms_fix_typeof_handling.patch \
    file://0002-cp1emu-do-not-use-bools-for-arithmetic.patch \
    file://0003-log2-give-up-on-gcc-constant-optimizations.patch \
    http://source.mynonpublic.com/azbox/initramfs-${MACHINE}-oe-core-${KV}-${SRCDATE}.tar.bz2;name=azbox-initrd-${MACHINE} \
    file://hdide.patch \
    "

SRC_URI[azbox-kernel.md5sum] = "dfd04abeaf3741b3d2a44428ca5aeaa1"
SRC_URI[azbox-kernel.sha256sum] = "31b73397220d85aedf3c914026371fc1eeac67e3de09a5610b70b209d2a8b9df"

SRC_URI[azbox-kernel-azboxhd.md5sum] = "df2576b23a9fb81f218fb6c24340805f"
SRC_URI[azbox-kernel-azboxhd.sha256sum] = "92d20d33aa457061ec859ad14ac4a856728f680b470d2c0143cd8837226c8fe6"
SRC_URI[azbox-initrd-azboxhd.md5sum] = "be250b8a23c782ba569ebaa65956d7e1"
SRC_URI[azbox-initrd-azboxhd.sha256sum] = "2cd4c203ac1f321c8b4f4f011411a5f987b3ea64e61f16ce6df73e9e15d39d4f"

S = "${WORKDIR}/linux-${KV}"
B = "${WORKDIR}/build"

CFLAGS_prepend = "-I${WORKDIR} "

export OS = "Linux"
KERNEL_OBJECT_SUFFIX = "ko"
KERNEL_OUTPUT = "zbimage-linux-xload"
KERNEL_IMAGETYPE = "zbimage-linux-xload"
KERNEL_IMAGEDEST = "tmp"

FILES_kernel-image = "/boot/zbimage-linux-xload"
EXTRA_OEMAKE =+ " CONFIG_INITRAMFS_SOURCE=${STAGING_KERNEL_DIR}/initramfs"

do_configure_prepend() {
    sed -i "s#:= usr/initramfs_default_node_list#:= \$(srctree)/usr/initramfs_default_node_list#" ${STAGING_KERNEL_DIR}/usr/Makefile
    sed -i "s#\$(srctree)/arch/mips/boot/#\$(obj)/#" ${STAGING_KERNEL_DIR}/arch/mips/boot/Makefile
}

kernel_do_compile_prepend() {
    gcc ${CFLAGS} ${WORKDIR}/genzbf.c -o ${WORKDIR}/genzbf
    install -d ${B}/arch/${ARCH}/boot/
    install -m 0755 ${WORKDIR}/genzbf ${B}/arch/${ARCH}/boot/
}

kernel_do_compile() {
    unset CFLAGS CPPFLAGS CXXFLAGS LDFLAGS MACHINE
    oe_runmake ${KERNEL_IMAGETYPE} CC="${KERNEL_CC}" LD="${KERNEL_LD}" ${KERNEL_EXTRA_ARGS}
}

kernel_do_compile_append() {
    rm -rf ${B}/arch/${ARCH}/boot/genzbf
    rm -rf ${B}/arch/${ARCH}/boot/${KERNEL_IMAGETYPE}
    install -m 0644 ${WORKDIR}/zbimage-linux-xload ${B}/arch/${ARCH}/boot/${KERNEL_IMAGETYPE}
}

# This is part of kernel.bbclass but doesn't get executed when not copied here
do_sizecheck() {
        if [ ! -z "${KERNEL_IMAGE_MAXSIZE}" ]; then
                cd ${B}
                size=`ls -lL ${KERNEL_OUTPUT} | awk '{ print $5}'`
                if [ $size -ge ${KERNEL_IMAGE_MAXSIZE} ]; then
                        die "This kernel (size=$size > ${KERNEL_IMAGE_MAXSIZE}) is too big for your device. Please reduce the size of the kernel by making more of it modular."
                fi
        fi
}
do_sizecheck[dirs] = "${B}"

addtask sizecheck before do_install after do_strip

do_rm_work() {
}

# extra tasks
addtask kernel_link_images after do_compile before do_install
