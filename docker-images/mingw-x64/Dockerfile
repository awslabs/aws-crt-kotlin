# FROM dockcross/windows-shared-x64:20250324-a3b42cd
FROM dockcross/windows-static-x64:20250324-a3b42cd
ENV DEFAULT_DOCKCROSS_IMAGE aws-crt-kotlin/mingw-x64:latest

RUN curl -LJo konan-toolchain.tar.gz https://download.jetbrains.com/kotlin/native/msys2-mingw-w64-x86_64-2.tar.gz
# Remove the cross compile root from the original image and put our own one based
# on the K/N toolchain.
#RUN mv /usr/src/mxe/usr/x86_64-w64-mingw32.static /tmp/original/ && \
#    mkdir -p /usr/src/mxe/usr/x86_64-w64-mingw32 && \
#    tar -xzf konan-toolchain.tar.gz -C /usr/src/mxe/usr/x86_64-w64-mingw32 --strip-components=1 && \
#    rm konan-toolchain.tar.gz
#
#ENV MXE_PREFIX /usr/src/mxe/usr
#ENV CROSS_TRIPLE x86_64-w64-mingw32
#ENV CROSS_ROOT ${MXE_PREFIX}/${CROSS_TRIPLE}
#RUN chmod 755 ${CROSS_ROOT}/bin/*.exe ${CROSS_ROOT}/${CROSS_TRIPLE}/bin/*.exe
#ENV PATH ${PATH}:${CROSS_ROOT}/bin:${CROSS_ROOT}/${CROSS_TRIPLE}/bin
#
#ENV AS=${CROSS_TRIPLE}.static-as \
#    AR=${CROSS_TRIPLE}.static-ar \
#    CC=${CROSS_TRIPLE}.static-gcc \
#    CPP=${CROSS_TRIPLE}.static-cpp \
#    CXX=${CROSS_TRIPLE}.static-g++ \
#    LD=${CROSS_TRIPLE}.static-ld \
#    FC=${CROSS_TRIPLE}.static-gfortran \
#    STRIP=${CROSS_TRIPLE}.static-strip \
#    OBJCOPY=${CROSS_TRIPLE}.static-objcopy \
#    CFLAGS="-I ${CROSS_ROOT}/include/ -I ${CROSS_ROOT}/${CROSS_TRIPLE}/include/ -L ${CROSS_ROOT}/lib/ -L ${CROSS_ROOT}/${CROSS_TRIPLE}/lib/" \
#    LDFLAGS="-L ${CROSS_ROOT}/lib/ -L ${CROSS_ROOT}/${CROSS_TRIPLE}/lib"
#
#COPY docker-images/mingw-x64/Toolchain.cmake ${CROSS_ROOT}/
#ENV CMAKE_TOOLCHAIN_FILE ${CROSS_ROOT}/Toolchain.cmake
