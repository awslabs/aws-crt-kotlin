## AWS CRT Kotlin

Kotlin bindings to the AWS Common Runtime

[![License][apache-badge]][apache-url]

[apache-badge]: https://img.shields.io/badge/License-Apache%202.0-blue.svg
[apache-url]: LICENSE

## License

This project is licensed under the Apache-2.0 License.

## Building

Kotlin Multiplatform projects are in [Alpha](https://kotlinlang.org/docs/reference/evolution/components-stability.html). The CRT interfaces are subject to change.

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

