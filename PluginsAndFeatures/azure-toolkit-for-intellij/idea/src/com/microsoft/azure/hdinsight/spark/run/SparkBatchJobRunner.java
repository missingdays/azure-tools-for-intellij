/*
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
 *
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.google.common.collect.ImmutableList;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azure.hdinsight.spark.run.action.SparkBatchJobDisconnectAction;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.telemetry.TelemetryKeys;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observer;
import rx.subjects.PublishSubject;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SparkBatchJobRunner extends DefaultProgramRunner implements SparkSubmissionRunner, ILogger {
    @NotNull
    @Override
    public String getRunnerId() {
        return "SparkBatchJobRun";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return SparkBatchJobRunExecutor.EXECUTOR_ID.equals(executorId)
                && profile.getClass() == LivySparkBatchJobRunConfiguration.class;
    }

    @Override
    @NotNull
    public ISparkBatchJob buildSparkBatchJob(@NotNull SparkSubmitModel submitModel,
                                             @NotNull Observer<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) throws ExecutionException {
        String clusterName = submitModel.getSubmissionParameter().getClusterName();
        IClusterDetail clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName)
                .orElseThrow(() -> new ExecutionException("Can't find cluster named " + clusterName));

        Deployable jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(submitModel, ctrlSubject);
        return new SparkBatchJob(clusterDetail, submitModel.getSubmissionParameter(), SparkBatchSubmission.getInstance(), ctrlSubject, jobDeploy);
    }

    protected void addConsoleViewFilter(@NotNull ISparkBatchJob job, @NotNull ConsoleView consoleView) {
    }

    protected void sendTelemetryForParameters(@NotNull SparkSubmitModel model, @Nullable Operation operation) {
        try {
            SparkSubmissionParameter params = model.getSubmissionParameter();
            Map<String, String> props = new HashMap<>();

            if (params.getDriverCores() != null) {
                props.put(SparkSubmissionParameter.DriverCores, params.getDriverCores().toString());
            }

            if (params.getDriverMemory() != null) {
                props.put(SparkSubmissionParameter.DriverMemory, params.getDriverMemory());
            }

            if (params.getExecutorCores() != null) {
                props.put(SparkSubmissionParameter.ExecutorCores, params.getExecutorCores().toString());
            }

            if (params.getExecutorMemory() != null) {
                props.put(SparkSubmissionParameter.ExecutorMemory, params.getExecutorMemory());
            }

            if (params.getNumExecutors() != null) {
                props.put(SparkSubmissionParameter.NumExecutors, params.getNumExecutors().toString());
            }

            props.put("refJarsCount", String.valueOf(Optional.ofNullable(params.getReferencedJars()).orElse(ImmutableList.of()).size()));
            props.put("refFilesCount", String.valueOf(Optional.ofNullable(params.getReferencedFiles()).orElse(ImmutableList.of()).size()));
            props.put("commandlineArgsCount", String.valueOf(Optional.ofNullable(params.getArgs()).orElse(ImmutableList.of()).size()));
            props.put("isLocalArtifact", String.valueOf(model.getIsLocalArtifact()));
            props.put("isDefaultArtifact",
                    model.getIsLocalArtifact()
                            ? "none"
                            : String.valueOf(Optional.ofNullable(model.getArtifactName()).orElse("").toLowerCase().endsWith("defaultartifact")));
            EventUtil.logEvent(EventType.info, operation, props);
        } catch (Exception ignored) {
            log().warn("Error sending telemetry when submit spark jobs. " + ExceptionUtils.getStackTrace(ignored));
        }
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        SparkBatchRemoteRunProfileState submissionState = (SparkBatchRemoteRunProfileState) state;

        SparkSubmitModel submitModel = submissionState.getSubmitModel();
        Project project = submitModel.getProject();

        // Prepare the run table console view UI
        SparkJobLogConsoleView jobOutputView = new SparkJobLogConsoleView(project);

        String artifactPath = submitModel.getArtifactPath().orElse(null);
        assert artifactPath != null : "artifactPath should be checked in LivySparkBatchJobRunConfiguration::checkSubmissionConfigurationBeforeRun";

        PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject = PublishSubject.create();
        SparkBatchJobRemoteProcess remoteProcess = new SparkBatchJobRemoteProcess(
                new IdeaSchedulers(project),
                buildSparkBatchJob(submitModel, ctrlSubject),
                artifactPath,
                submitModel.getSubmissionParameter().getMainClassName(),
                ctrlSubject);
        SparkBatchJobRunProcessHandler processHandler = new SparkBatchJobRunProcessHandler(remoteProcess, "Package and deploy the job to Spark cluster", null);

        // After attaching, the console view can read the process inputStreams and display them
        jobOutputView.attachToProcess(processHandler);

        remoteProcess.start();
        Operation operation = environment.getUserData(TelemetryKeys.OPERATION);
        SparkBatchJobDisconnectAction disconnectAction = new SparkBatchJobDisconnectAction(remoteProcess, operation);

        sendTelemetryForParameters(submitModel, operation);

        ExecutionResult result = new DefaultExecutionResult(jobOutputView, processHandler, Separator.getInstance(), disconnectAction);
        submissionState.setExecutionResult(result);
        submissionState.setConsoleView(jobOutputView.getSecondaryConsoleView());

        addConsoleViewFilter(remoteProcess.getSparkJob(), submissionState.getConsoleView());

        submissionState.setRemoteProcessCtrlLogHandler(processHandler);

        ctrlSubject.subscribe(
                messageWithType -> {
                },
                err -> disconnectAction.setEnabled(false),
                () -> disconnectAction.setEnabled(false));

        return super.doExecute(state, environment);
    }

    @Override
    public void setFocus(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof LivySparkBatchJobRunConfiguration) {
            LivySparkBatchJobRunConfiguration livyRunConfig = (LivySparkBatchJobRunConfiguration) runConfiguration;
            livyRunConfig.getModel().setFocusedTabIndex(1);
        }
    }
}
