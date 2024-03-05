cmake_minimum_required(VERSION 3.1)
project(aws-crt-kotlin C)
message(STATUS "CMake ${CMAKE_VERSION}")

option(BUILD_DEPS "Builds aws common runtime dependencies as part of build. Turn off if you want to control your dependency chain." ON)
option(BUILD_SHARED_LIBS "Build shared library for FFI: default: ON" ON)

if (POLICY CMP0069)
    cmake_policy(SET CMP0069 NEW) # Enable LTO/IPO if available in the compiler
endif()

if (DEFINED CMAKE_PREFIX_PATH)
    file(TO_CMAKE_PATH "${CMAKE_PREFIX_PATH}" CMAKE_PREFIX_PATH)
endif()

if (NOT DEFINED CMAKE_INSTALL_LIBDIR)
    set(CMAKE_INSTALL_LIBDIR "lib")
endif()

if (UNIX AND NOT APPLE)
    set(FIND_LIBRARY_USE_LIB64_PATHS true)
endif()

# This is required in order to append /lib/cmake to each element in CMAKE_PREFIX_PATH
set(AWS_MODULE_DIR "/${CMAKE_INSTALL_LIBDIR}/cmake")
string(REPLACE ";" "${AWS_MODULE_DIR};" AWS_MODULE_PATH "${CMAKE_PREFIX_PATH}${AWS_MODULE_DIR}")
# Append that generated list to the module search path
list(APPEND CMAKE_MODULE_PATH ${AWS_MODULE_PATH})


if (BUILD_DEPS)
    list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/crt/aws-c-common/cmake")

    include(AwsFindPackage)

    set(IN_SOURCE_BUILD ON)
    set(SEARCH_LIBCRYPTO OFF CACHE BOOL "Let S2N use libcrypto from AWS-LC.")

    # Don't compile tests in subdirectories.
    # Turn off using `option` instead of `set`, or CTest will declare
    # it as an option later and override the existing variable.
    set(BUILD_TESTING OFF)

    # Disable BUILD_SHARED_LIBS for all CRT libs
    set(SHARED_FFI_LIB ${BUILD_SHARED_LIBS})
    set(BUILD_SHARED_LIBS OFF)

    # CRT Libraries
    add_subdirectory(crt/aws-c-common)
    if (UNIX AND NOT APPLE)
        if (NOT USE_OPENSSL)
            if (NOT (CMAKE_SYSTEM_PROCESSOR STREQUAL aarch64) AND
                NOT (CMAKE_SYSTEM_PROCESSOR STREQUAL armv7l))
                set(DISABLE_PERL ON)
            endif()
            if(CMAKE_C_COMPILER_ID MATCHES "GNU" AND CMAKE_C_COMPILER_VERSION VERSION_LESS "5.0")
                set(DISABLE_PERL OFF CACHE BOOL "Build with Perl to avoid using pre-compiled binary with AVX512")
                set(MY_ASSEMBLER_IS_TOO_OLD_FOR_512AVX ON CACHE BOOL "Disable AVX512 on old GCC that not supports it")
            endif()
            set(DISABLE_GO ON)
            set(SEARCH_LIBCRYPTO OFF)
            set(BUILD_LIBSSL OFF)
            add_subdirectory(crt/aws-lc)
        else()
            set(SEARCH_LIBCRYPTO ON)
            # Find the system libcrypto and propagate its location to s2n's find script
            find_package(OpenSSL REQUIRED)
            set(LibCrypto_INCLUDE_DIR ${OPENSSL_INCLUDE_DIR})
            set(LibCrypto_LIBRARY ${OPENSSL_CRYPTO_LIBRARY})
            if (LibCrypto_LIBRARY MATCHES ".so$")
                set(LibCrypto_SHARED_LIBRARY ${LibCrypto_LIBRARY})
            else()
                set(LibCrypto_STATIC_LIBRARY ${LibCrypto_LIBRARY})
            endif()
        endif()
        add_subdirectory(crt/s2n)
    endif()
    add_subdirectory(crt/aws-c-sdkutils)
    add_subdirectory(crt/aws-c-io)
    add_subdirectory(crt/aws-c-cal)
    add_subdirectory(crt/aws-c-compression)
    add_subdirectory(crt/aws-c-http)
    add_subdirectory(crt/aws-c-auth)
    add_subdirectory(crt/aws-checksums)
else()
    include(AwsFindPackage)
    set(IN_SOURCE_BUILD OFF)
endif()

# Restore BUILD_SHARED_LIBS for this project
set(BUILD_SHARED_LIBS ${SHARED_FFI_LIB})

include(AwsCFlags)
include(AwsSharedLibSetup)
include(AwsSanitizers)

aws_use_package(aws-c-common)
aws_use_package(aws-c-sdkutils)
aws_use_package(aws-c-io)
aws_use_package(aws-c-cal)
aws_use_package(aws-c-compression)
aws_use_package(aws-c-http)
aws_use_package(aws-c-auth)
aws_use_package(aws-checksums)

# TODO - bundling static libraries into single static library composed of all the static libraries
# to enable IPO/LTO. See also:
# * https://cristianadam.eu/20190501/bundling-together-static-libraries-with-cmake/
# * https://cmake.org/cmake/help/latest/module/CheckIPOSupported.html