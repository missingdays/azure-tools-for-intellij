/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure;

import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.AzureAppServiceModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.database.AzureDatabaseModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import java.util.List;
import java.util.stream.Collectors;

public class AzureModule extends AzureRefreshableNode {
    private static final String AZURE_SERVICE_MODULE_ID = AzureModule.class.getName();
    private static final String ICON_PATH = "AzureExplorer.svg";
    private static final String BASE_MODULE_NAME = "Azure";

    @Nullable
    private Object project;
    @NotNull
    private VMArmModule vmArmServiceModule;
    @NotNull
    private RedisCacheModule redisCacheModule;
    @NotNull
    private StorageModule storageModule;
    @Nullable
    private HDInsightRootModule hdInsightModule;
    @Nullable
    private HDInsightRootModule sparkServerlessClusterRootModule;
    @NotNull
    private DockerHostModule dockerHostModule;
    @NotNull
    private ContainerRegistryModule containerRegistryModule;
    @NotNull
    private AzureDatabaseModule azureDatabaseModule;
    @NotNull
    private AzureAppServiceModule azureAppServiceModule;
    @NotNull
    private ResourceManagementModule resourceManagementModule;
    /**
     * Constructor.
     *
     * @param project project
     */
    public AzureModule(@Nullable Object project) {
        super(AZURE_SERVICE_MODULE_ID, composeName(), null, ICON_PATH);
        this.project = project;
        storageModule = new StorageModule(this);
        //hdInsightModule = new HDInsightRootModule(this);
        vmArmServiceModule = new VMArmModule(this);
        redisCacheModule = new RedisCacheModule(this);
        dockerHostModule = new DockerHostModule(this);
        containerRegistryModule = new ContainerRegistryModule(this);
        azureDatabaseModule = new AzureDatabaseModule(this);
        azureAppServiceModule = new AzureAppServiceModule(this);
        resourceManagementModule = new ResourceManagementModule(this);
        try {
            SignInOutListener signInOutListener = new SignInOutListener();
            AuthMethodManager.getInstance().addSignInEventListener(signInOutListener);
            AuthMethodManager.getInstance().addSignOutEventListener(signInOutListener);
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
        // in case we already signed in with service principal between restarts, sign in event was not fired
        addSubscriptionSelectionListener();
    }

    private static String composeName() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return BASE_MODULE_NAME + " (Not Signed In)";
            }
            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
            List<SubscriptionDetail> selectedSubscriptions = subscriptionDetails.stream()
                    .filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
            if (selectedSubscriptions.size() > 0) {
                return String.format("%s (%s)", BASE_MODULE_NAME, selectedSubscriptions.size() > 1
                        ? String.format("%s subscriptions", selectedSubscriptions.size())
                        : selectedSubscriptions.get(0).getSubscriptionName());
            }
        } catch (Throwable e) {
            String msg = "An error occurred while getting the subscription list." + "\n" + "(Message from Azure:" + e
                    .getMessage() + ")";
            DefaultLoader.getUIHelper().logError(msg, e);
        }
        return BASE_MODULE_NAME;
    }

    public void setHdInsightModule(@NotNull HDInsightRootModule rootModule) {
        this.hdInsightModule = rootModule;
    }

    public void setSparkServerlessModule(@NotNull HDInsightRootModule rootModule) {
        this.sparkServerlessClusterRootModule = rootModule;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // add the module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item
        if (!isDirectChild(vmArmServiceModule)) {
            addChildNode(vmArmServiceModule);
        }
        if (!isDirectChild(redisCacheModule)) {
            addChildNode(redisCacheModule);
        }
        if (!isDirectChild(storageModule)) {
            addChildNode(storageModule);
        }
        if (!isDirectChild(resourceManagementModule)) {
            addChildNode(resourceManagementModule);
        }
        if (hdInsightModule != null && !isDirectChild(hdInsightModule)) {
            addChildNode(hdInsightModule);
        }

        if (sparkServerlessClusterRootModule != null &&
                sparkServerlessClusterRootModule.isFeatureEnabled() &&
                !isDirectChild(sparkServerlessClusterRootModule)) {
            addChildNode(sparkServerlessClusterRootModule);
        }

        if (!isDirectChild(dockerHostModule)) {
            addChildNode(dockerHostModule);
        }
        if (!isDirectChild(containerRegistryModule)) {
            addChildNode(containerRegistryModule);
        }
        if (!isDirectChild(azureDatabaseModule)) {
            addChildNode(azureDatabaseModule);
        }
        if (!isDirectChild(azureAppServiceModule)) {
            addChildNode(azureAppServiceModule);
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        super.onNodeClick(e);
    }

    @Override
    protected void refreshFromAzure() throws AzureCmdException {
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                vmArmServiceModule.load(true);
                redisCacheModule.load(true);
                storageModule.load(true);
                resourceManagementModule.load(true);

                if (hdInsightModule != null) {
                    hdInsightModule.load(true);
                }

                if (sparkServerlessClusterRootModule != null) {
                    sparkServerlessClusterRootModule.load(true);
                }

                dockerHostModule.load(true);
                containerRegistryModule.load(true);

                azureDatabaseModule.load(true);
                azureAppServiceModule.load(true);
            }
        } catch (Exception e) {
            throw new AzureCmdException("Error loading Azure Explorer modules", e);
        }
    }

    @Nullable
    @Override
    public Object getProject() {
        return project;
    }

    private void addSubscriptionSelectionListener() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }
            azureManager.getSubscriptionManager().addListener(isRefresh -> {
                if (!isRefresh) {
                    handleSubscriptionChange();
                }
            });
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }

    private void handleSubscriptionChange() {
        setName(composeName());
        for (Node child : getChildNodes()) {
            child.removeAllChildNodes();
        }
    }

    private class SignInOutListener implements Runnable {
        @Override
        public void run() {
            handleSubscriptionChange();
            addSubscriptionSelectionListener();
        }
    }
}
