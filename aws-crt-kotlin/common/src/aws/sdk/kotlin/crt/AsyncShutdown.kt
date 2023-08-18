/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

public interface AsyncShutdown {
    /**
     * Suspend until the resource has completed its asynchronous shutdown
     */
    public suspend fun waitForShutdown()
}
