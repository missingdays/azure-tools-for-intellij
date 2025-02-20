/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.config.ui

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.runner.appbase.config.ui.AppDeployViewPresenterBase
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel

class DotNetWebAppDeployViewPresenter<V : DotNetWebAppDeployMvpView> : AppDeployViewPresenterBase<V>() {

    companion object {
        private const val TASK_WEB_APP = "Collect Azure web apps"
        private const val CANNOT_LIST_WEB_APP = "Failed to list web apps."
    }

    private val webAppSignal = Signal<List<ResourceEx<WebApp>>>()

    override fun loadApps(lifetime: Lifetime, forceRefresh: Boolean) {
        subscribe(lifetime, webAppSignal, TASK_WEB_APP, CANNOT_LIST_WEB_APP,
                { AzureDotNetWebAppMvpModel.listWebApps(forceRefresh) },
                { mvpView.fillExistingWebAppsTable(it) })
    }
}
