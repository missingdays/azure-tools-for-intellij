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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp

import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.*
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.base.FunctionAppBaseNodeView
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.base.FunctionAppState
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.functions.FunctionNode
import java.util.logging.Logger

class FunctionAppNode(parent: AzureFunctionAppModule,
                      override val subscriptionId: String,
                      override val functionAppId: String,
                      override val functionAppName: String,
                      override var state: String,
                      private val hostName: String) :
        RefreshableNode(functionAppId, functionAppName, parent, getFunctionAppIcon(state), true),
        FunctionAppVirtualInterface,
        FunctionAppBaseNodeView {

    companion object {
        private const val ACTION_START = "Start"
        private const val ACTION_STOP = "Stop"
        private const val ACTION_RESTART = "Restart"
        private const val ACTION_DELETE = "Delete"
        private const val ACTION_OPEN_IN_BROWSER = "Open In Browser"
        private const val ACTION_OPEN_PROPERTIES = "Open Properties"

        private const val PROGRESS_MESSAGE_DELETE_FUNCTION_APP = "Deleting Function App '%s'"
        private const val PROMPT_MESSAGE_DELETE_FUNCTION_APP =
                "This operation will delete Function App '%s'.\nAre you sure you want to continue?"

        private const val ICON_FUNCTION_APP_RUNNING = "FunctionAppRunning.svg"
        private const val ICON_FUNCTION_APP_STOPPED = "FunctionAppStopped.svg"
        private const val ICON_ACTION_START = "AzureStart.svg"
        private const val ICON_ACTION_STOP = "AzureStop.svg"
        private const val ICON_ACTION_RESTART = "AzureRestart.svg"
        private const val ICON_ACTION_OPEN_IN_BROWSER = "OpenInBrowser.svg"
        private const val ICON_ACTION_DELETE = "Discard.svg"
        private const val ICON_ACTION_SHOW_PROPERTIES = "gearPlain.svg"

        private fun getFunctionAppIcon(state: String) =
                if (FunctionAppState.fromString(state) == FunctionAppState.RUNNING) ICON_FUNCTION_APP_RUNNING
                else ICON_FUNCTION_APP_STOPPED
    }

    private val logger = Logger.getLogger(FunctionAppNode::class.java.name)

    private val presenter = FunctionAppNodePresenter<FunctionAppNode>()
    private val startAction: NodeAction
    private val stopAction: NodeAction

    init {
        startAction = NodeAction(this, ACTION_START)
        startAction.iconPath = ICON_ACTION_START
        startAction.addListener(createBackgroundActionListener("Starting Function App") { startFunctionApp() })

        stopAction = NodeAction(this, ACTION_STOP)
        stopAction.iconPath = ICON_ACTION_STOP
        stopAction.addListener(createBackgroundActionListener("Stopping Function App") { stopFunctionApp() })

        loadActions()
        presenter.onAttachView(this@FunctionAppNode)
    }

    override fun onError(message: String) {
    }

    override fun onErrorWithException(message: String, ex: Exception) {
    }

    override fun loadActions() {
        addAction(ACTION_RESTART, ICON_ACTION_RESTART, createBackgroundActionListener("Restarting Function App") { restartFunctionApp() })
        addAction(ACTION_DELETE, ICON_ACTION_DELETE, DeleteFunctionAppAction())
        addAction(ACTION_OPEN_IN_BROWSER, ICON_ACTION_OPEN_IN_BROWSER, OpenInBrowserAction())
        addAction(ACTION_OPEN_PROPERTIES, ICON_ACTION_SHOW_PROPERTIES, OpenProperties())
        super.loadActions()
    }

    override fun refreshItems() {
        val functions = AzureFunctionAppMvpModel.listFunctionsForAppWithId(subscriptionId, functionAppId)

        for (function in functions) {
            val functionId = function.id()
            val functionName = function.name()
            val isEnabled = function.isEnabled()

            addChildNode(FunctionNode(this, subscriptionId, isEnabled, functionId, functionName))
        }
    }

    override fun renderNode(state: FunctionAppState) {
        when (state) {
            FunctionAppState.RUNNING -> {
                this.state = state.name
                setIconPath(ICON_FUNCTION_APP_RUNNING)
            }
            FunctionAppState.STOPPED -> {
                this.state = state.name
                setIconPath(ICON_FUNCTION_APP_STOPPED)
            }
        }
    }

    override fun getNodeActions(): MutableList<NodeAction> {
        val isRunning = FunctionAppState.fromString(state) == FunctionAppState.RUNNING

        val stopAction = getNodeActionByName(ACTION_STOP)
        val startAction = getNodeActionByName(ACTION_START)
        val restartAction = getNodeActionByName(ACTION_RESTART)

        if (isRunning) {
            if (startAction != null)
                nodeActions.remove(startAction)

            if (stopAction == null)
                nodeActions.add(0, this.stopAction)
        } else {
            if (stopAction != null)
                nodeActions.remove(stopAction)

            if (startAction == null)
                nodeActions.add(0, this.startAction)
        }

        restartAction.isEnabled = isRunning

        return super.getNodeActions()
    }

    private fun createBackgroundActionListener(actionName: String, action: () -> Unit) =
            object : NodeActionListener() {
                override fun actionPerformed(event: NodeActionEvent) {
                    DefaultLoader.getIdeHelper().runInBackground(null, actionName, false, true, "$actionName...", action)
                }
            }

    private fun stopFunctionApp() =
            try { presenter.onStopFunctionApp(subscriptionId, functionAppId) }
            catch (t: Throwable) { logger.warning("Error while stopping Function App with Id: $functionAppId: $t") }

    private fun startFunctionApp() =
            try { presenter.onStartFunctionApp(subscriptionId, functionAppId) }
            catch(t: Throwable) { logger.warning("Error while starting Function App with Id: $functionAppId: $t") }

    private fun restartFunctionApp() =
            try { presenter.onRestartFunctionApp(subscriptionId, functionAppId) }
            catch(t: Throwable) { logger.warning("Error while restarting Function App with Id: $functionAppId: $t") }

    private inner class DeleteFunctionAppAction internal constructor() : AzureNodeActionPromptListener(
            this@FunctionAppNode,
            String.format(PROMPT_MESSAGE_DELETE_FUNCTION_APP, functionAppName),
            String.format(PROGRESS_MESSAGE_DELETE_FUNCTION_APP, functionAppName)) {

        override fun azureNodeAction(event: NodeActionEvent?) {
            try {
                AzureFunctionAppMvpModel.deleteFunctionApp(subscriptionId, functionAppId)
                DefaultLoader.getIdeHelper().invokeLater {
                    getParent().removeNode(subscriptionId, functionAppId, this@FunctionAppNode)
                }
            } catch (t: Throwable) {
                DefaultLoader.getUIHelper().logError(t.message, t)
            }
        }

        override fun onSubscriptionsChanged(e: NodeActionEvent?) {
        }
    }

    private inner class OpenInBrowserAction internal constructor() : NodeActionListener() {

        override fun actionPerformed(e: NodeActionEvent?) {
            DefaultLoader.getUIHelper().openInBrowser("http://$hostName")
        }
    }

    private inner class OpenProperties internal constructor() : NodeActionListener() {

        override fun actionPerformed(e: NodeActionEvent?) {
            DefaultLoader.getUIHelper().openFunctionAppProperties(this@FunctionAppNode)
        }
    }
}
