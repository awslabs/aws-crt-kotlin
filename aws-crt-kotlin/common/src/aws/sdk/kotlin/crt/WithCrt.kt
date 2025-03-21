package aws.sdk.kotlin.crt

/**
 * A mixin class used to ensure CRT is initialized before the class is invoked
 */
public open class WithCrt {
    init {
        CRT.initRuntime { }
    }
}