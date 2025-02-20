/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config.ui

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import com.microsoft.intellij.runner.appbase.config.ui.AppDeployViewPresenterBase
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType

class FunctionAppDeployViewPresenter<V : FunctionAppDeployMvpView> : AppDeployViewPresenterBase<V>() {

    companion object {
        private const val TASK_FUNCTION_APP = "Collect Azure function apps"
        private const val TASK_STORAGE_ACCOUNT = "Collect Azure Storage Accounts"
        private const val TASK_STORAGE_ACCOUNT_TYPE = "Collect Azure Storage Account Types"

        private const val CANNOT_LIST_FUNCTION_APP = "Failed to list function apps."
        private const val CANNOT_LIST_STORAGE_ACCOUNT = "Failed to list storage accounts."
        private const val CANNOT_LIST_STORAGE_ACCOUNT_TYPE = "Failed to list storage account types."
    }

    private val functionAppSignal = Signal<List<ResourceEx<FunctionApp>>>()
    private val storageAccountSignal = Signal<List<StorageAccount>>()
    private val storageAccountTypeSignal = Signal<List<StorageAccountSkuType>>()

    override fun loadApps(lifetime: Lifetime, forceRefresh: Boolean) {
        subscribe(lifetime, functionAppSignal, TASK_FUNCTION_APP, CANNOT_LIST_FUNCTION_APP,
                { AzureFunctionAppMvpModel.listAllFunctionApps(forceRefresh) },
                { functions -> mvpView.fillFunctionAppsTable(functions) })
    }

    fun onLoadStorageAccounts(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime, storageAccountSignal, TASK_STORAGE_ACCOUNT, CANNOT_LIST_STORAGE_ACCOUNT,
                { AzureStorageAccountMvpModel.listStorageAccountsBySubscriptionId(subscriptionId, true) },
                { storageAccounts -> mvpView.fillStorageAccount(storageAccounts) })
    }

    fun onLoadStorageAccountTypes(lifetime: Lifetime) {
        subscribe(lifetime, storageAccountTypeSignal, TASK_STORAGE_ACCOUNT_TYPE, CANNOT_LIST_STORAGE_ACCOUNT_TYPE,
                { AzureStorageAccountMvpModel.listStorageAccountType() },
                { types -> mvpView.fillStorageAccountType(types) })
    }
}
