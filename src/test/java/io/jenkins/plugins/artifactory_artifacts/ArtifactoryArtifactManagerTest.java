package io.jenkins.plugins.artifactory_artifacts;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import hudson.util.FormValidation;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WireMockTest
public class ArtifactoryArtifactManagerTest extends BaseTest {

    @Test
    public void shouldDeleteArtifactWhenDeletingJob(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo.getHttpPort(), "");

        String pipelineName = "shouldDeleteArtifactWhenDeletingJob";

        // Create pipeline and run it
        String pipeline = IOUtils.toString(
                Objects.requireNonNull(PipelineTest.class.getResourceAsStream("/pipelines/archiveController.groovy")),
                StandardCharsets.UTF_8);

        // Setup wiremock stubs
        setupWireMockStubs(
                pipelineName,
                wmRuntimeInfo.getWireMock(),
                wmRuntimeInfo.getHttpPort(),
                "",
                "artifact.txt",
                "stash.tgz");

        // Query job folder
        String folderPath = "/api/storage/my-generic-repo/" + pipelineName;
        String jobFolderResponse = "{"
                + "\"children\": [{\"folder\": true, \"uri\": \"/1\"}],"
                + "\"created\": \"2024-03-17T13:20:19.836Z\","
                + "\"createdBy\": \"admin\","
                + "\"lastModified\": \"2024-03-17T13:20:19.836Z\","
                + "\"lastUpdated\": \"2024-03-17T13:20:19.836Z\","
                + "\"modifiedBy\": \"admin\","
                + "\"path\": \"" + folderPath + "\","
                + "\"repo\": \"my-generic-repo\","
                + "\"uri\": \"http://localhost:" + wmRuntimeInfo.getHttpPort() + "/artifactory" + folderPath + "\""
                + "}";
        wmRuntimeInfo
                .getWireMock()
                .register(WireMock.get(WireMock.urlEqualTo(folderPath)).willReturn(WireMock.okJson(jobFolderResponse)));

        // Delete the folder
        wmRuntimeInfo
                .getWireMock()
                .register(WireMock.delete(WireMock.urlEqualTo("/my-generic-repo/" + pipelineName))
                        .willReturn(WireMock.ok()));

        // Run job
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, pipelineName);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = Objects.requireNonNull(workflowJob.scheduleBuild2(0)).waitForStart();
        jenkinsRule.waitForCompletion(run1);

        // Job success
        assertThat(run1.getResult(), equalTo(hudson.model.Result.SUCCESS));

        // Delete job
        workflowJob.delete();
    }

    @Test
    public void shouldDeleteArtifactWhenDeletingBuild(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo.getHttpPort(), "");

        String pipelineName = "shouldDeleteArtifactWhenDeletingBuild";

        // Create pipeline and run it
        String pipeline = IOUtils.toString(
                Objects.requireNonNull(PipelineTest.class.getResourceAsStream("/pipelines/archiveController.groovy")),
                StandardCharsets.UTF_8);

        // Setup wiremock stubs
        setupWireMockStubs(
                pipelineName,
                wmRuntimeInfo.getWireMock(),
                wmRuntimeInfo.getHttpPort(),
                "",
                "artifact.txt",
                "stash.tgz");

        // Query build folder
        String folderPath = "/api/storage/my-generic-repo/" + pipelineName + "/1";
        String jobFolderResponse = "{"
                + "\"children\": [{\"folder\": true, \"uri\": \"/artifacts\"}],"
                + "\"created\": \"2024-03-17T13:20:19.836Z\","
                + "\"createdBy\": \"admin\","
                + "\"lastModified\": \"2024-03-17T13:20:19.836Z\","
                + "\"lastUpdated\": \"2024-03-17T13:20:19.836Z\","
                + "\"modifiedBy\": \"admin\","
                + "\"path\": \"" + folderPath + "\","
                + "\"repo\": \"my-generic-repo\","
                + "\"uri\": \"http://localhost:" + wmRuntimeInfo.getHttpPort() + "/artifactory" + folderPath + "\""
                + "}";
        wmRuntimeInfo
                .getWireMock()
                .register(WireMock.get(WireMock.urlEqualTo(folderPath)).willReturn(WireMock.okJson(jobFolderResponse)));

        // Delete the build folder
        wmRuntimeInfo
                .getWireMock()
                .register(WireMock.delete(WireMock.urlEqualTo("/my-generic-repo/" + pipelineName + "/1/"))
                        .willReturn(WireMock.ok()));

        // Run job
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, pipelineName);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = Objects.requireNonNull(workflowJob.scheduleBuild2(0)).waitForStart();
        jenkinsRule.waitForCompletion(run1);

        // Job success
        assertThat(run1.getResult(), equalTo(hudson.model.Result.SUCCESS));

        // Delete job
        workflowJob.getLastBuild().delete();
    }

    @Test
    public void shouldDoValidateArtifactoryConfig(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo.getHttpPort(), "/jenkins");
        ArtifactoryGenericArtifactConfig.DescriptorImpl descriptor =
                jenkinsRule.getInstance().getDescriptorByType(ArtifactoryGenericArtifactConfig.DescriptorImpl.class);

        // WireMock stub
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // PUT and delete to validate
        wireMock.register(WireMock.put(WireMock.urlMatching("/my-generic-repo/jenkins/tmp-.*-artifactory-plugin-test"))
                .willReturn(WireMock.okJson("{}")));
        wireMock.register(
                WireMock.delete(WireMock.urlMatching("/my-generic-repo/jenkins/tmp-.*-artifactory-plugin-test"))
                        .willReturn(WireMock.okJson("{}")));

        // Test
        FormValidation validation = descriptor.doValidateArtifactoryConfig(
                config.getServerUrl(), config.getStorageCredentialId(), config.getRepository(), config.getPrefix());

        // Assert
        assertThat(validation.kind, is(FormValidation.Kind.OK));
    }

    @Test
    public void shouldFailToDoValidateArtifactoryConfig(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo.getHttpPort(), "jenkins/");
        ArtifactoryGenericArtifactConfig.DescriptorImpl descriptor =
                jenkinsRule.getInstance().getDescriptorByType(ArtifactoryGenericArtifactConfig.DescriptorImpl.class);

        // WireMock stub
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // 404 not found
        wireMock.register(WireMock.put(WireMock.urlMatching("/my-generic-repo/jenkins/tmp-.*-artifactory-plugin-test"))
                .willReturn(WireMock.notFound()));

        // Test
        FormValidation validation = descriptor.doValidateArtifactoryConfig(
                config.getServerUrl(), config.getStorageCredentialId(), config.getRepository(), config.getPrefix());

        // Assert
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(
                validation.getMessage(),
                startsWith("Unable to connect to Artifactory. Please check the server url and credentials"));
    }

    @Test
    public void shouldCreteCorrectFactory(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo.getHttpPort(), "jenkins/");
        ArtifactoryArtifactManagerFactory factory = new ArtifactoryArtifactManagerFactory(config);
        ArtifactoryArtifactManagerFactory.DescriptorImpl descriptor =
                jenkinsRule.getInstance().getDescriptorByType(ArtifactoryArtifactManagerFactory.DescriptorImpl.class);
        assertThat(factory.getConfig(), is(config));
        assertThat(descriptor.getDisplayName(), is("Artifactory Artifact Storage"));
    }

    @Test
    public void testConfigRoundtrip(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        // Set config
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo.getHttpPort(), "jenkins/");

        // Save config
        jenkinsRule.configRoundtrip();

        // Assert config
        assertThat(config.getStorageCredentialId(), is("the-credentials-id"));
        assertThat(config.getServerUrl(), is("http://localhost:" + wmRuntimeInfo.getHttpPort()));
        assertThat(config.getRepository(), is("my-generic-repo"));
        assertThat(config.getPrefix(), is("jenkins/"));
    }
}
