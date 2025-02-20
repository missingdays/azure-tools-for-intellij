/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.common.classifiedexception

import com.microsoft.azure.datalake.store.ADLException
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException

class SparkServiceException(exp: Throwable?) : ClassifiedException(exp) {
    override val title: String = "Spark Service Error"
    override val errorType = ErrorType.serviceError
}

object SparkServiceExceptionFactory : ClassifiedExceptionFactory() {
    override fun createClassifiedException(exp: Throwable?): ClassifiedException? {
        return if (exp is UnknownHostException) {
            val hintMsg = "\nPlease make sure that the cluster exists"
            SparkServiceException(UnknownHostException("${exp.message}$hintMsg"))
        } else if ((exp is IOException && exp !is FileNotFoundException && exp !is ADLException)
                || (exp is ADLException && exp.httpResponseCode != 403))
            SparkServiceException(exp) else null
    }
}
