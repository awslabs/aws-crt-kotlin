# We need glibc <= 2.19, see https://youtrack.jetbrains.com/issue/KT-47061
# TODO - explore creating a custom image using the same toolchain K/N uses
# see https://github.com/JetBrains/kotlin/tree/master/kotlin-native/tools/toolchain_builder
# OR via https://download.jetbrains.com/kotlin/native/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2.tar.gz
FROM dockcross/manylinux2014-x64:20240104-6eda627
ENV DEFAULT_DOCKCROSS_IMAGE aws-crt-kotlin/linux-x64:latest
