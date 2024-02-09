## AWS CRT Kotlin

Kotlin bindings to the AWS Common Runtime

[![License][apache-badge]][apache-url]

[apache-badge]: https://img.shields.io/badge/License-Apache%202.0-blue.svg
[apache-url]: LICENSE

## License

This project is licensed under the Apache-2.0 License.

## Building

CRT interfaces are subject to change.

### Linux/Unix
Install some version of libcrypto on which s2n depends. See the [s2n](https://github.com/awslabs/s2n) documentation.

```sh
apt-get install libssl-dev
```

OR

```sh
yum install openssl-devel
```

Set the path to `libcrypto.a` either as a command line argument to gradle `-PlibcryptoPath=PATH` or in your `local.properties` file.


### OSX


### Windows


## Elasticurl App

The `elasticurl` project contains an MPP (JVM and Native only) executable that provides a simple testing application for exercising the CRT bindings. 

**Native**

```
# replace "PLATFORM" with the target platform you want to run (e.g. macosX64, linuxX64, etc)

./elasticurl/bin/PLATFORM/elasticurl.kexe [OPTIONS] URL
```


**JVM**
```
java -jar ./elasticurl/libs/elasticurl-jvm.jar [OPTIONS] URL
```

NOTE: You can also use the convenience script `./scripts/elasticurlJvm.sh [OPTIONS] URL`

To enable memory tracing specify the environment variable `CRTDEBUG=trace=N` and provide the CLI option `-v trace`

e.g.
```
CRTDEBUG=trace=2 ./elasticurl/bin/macosX64/elasticurl.kexe -v trace https://aws.amazon.com
```


**Integration Test**

Run the simple elasticurl integration test script

`./scripts/elasticurl-test.sh`


### TODO

cmake >= 3.19.5

https://cmake.org/cmake/help/latest/manual/cmake-toolchains.7.html#cross-compiling-for-ios-tvos-visionos-or-watchos

```sh
docker build -f docker-images/linux-x64/Dockerfile -t aws-crt-kotlin/linux-x64:latest .
# update dockcross script
docker run --rm aws-crt-kotlin/linux-x64:latest > dockcross-linux-x64    
```

[ ] When to generate dockcross scripts? Seems like we should publish them to ECR and use that by default and have a mode to generate them
    locally?