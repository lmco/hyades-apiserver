/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) OWASP Foundation. All Rights Reserved.
 */
package org.dependencytrack.integrations.gitlab;

import jakarta.ws.rs.core.MediaType;
import net.minidev.json.JSONArray;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.dependencytrack.common.ConfigKeys;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.commons.io.IOUtils.resourceToString;

public class GitLabClientTest {

        @RegisterExtension
        static final WireMockExtension wireMock = WireMockExtension.newInstance()
                        .options(wireMockConfig().dynamicPort())
                        .build();

        @Test
        public void testConstructorWithAccessToken() {
                String accessToken = "my-access-token";
                GitLabClient client = new GitLabClient(accessToken);
                Assertions.assertThat(client).isNotNull();
        }

        @Test
        public void testConstructorWithAccessTokenAndConfig() {
                String accessToken = "my-access-token";
                Config config = ConfigProvider.getConfig();
                GitLabClient client = new GitLabClient(accessToken, config, null, false);
                Assertions.assertThat(client).isNotNull();
                Assertions.assertThat("Dependency-Track")
                                .isEqualTo(client.getConfig().getConfigValue(ConfigKeys.APPLICATION_NAME));
        }

        @Test
        public void testGetGitLabProjects() throws URISyntaxException, IOException {
                String accessToken = "TEST_ACCESS_TOKEN";

                String page1Result = resourceToString("/unit/gitlab-api-getgitlabprojects-response-page-1.json",
                                StandardCharsets.UTF_8);
                String page2Result = resourceToString("/unit/gitlab-api-getgitlabprojects-response-page-2.json",
                                StandardCharsets.UTF_8);

                stubFor(post(urlPathEqualTo("/api/graphql"))
                                .inScenario("test-get-gitlab-projects")
                                .whenScenarioStateIs(Scenario.STARTED)
                                .willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                                .withBody(page1Result))
                                .willSetStateTo("second-page"));

                stubFor(post(urlPathEqualTo("/api/graphql"))
                                .inScenario("test-get-gitlab-projects")
                                .whenScenarioStateIs("second-page")
                                .willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                                .withBody(page2Result))
                                .willSetStateTo("Finished"));

                Config config = ConfigProvider.getConfig();
                GitLabClient gitLabClient = new GitLabClient(accessToken, config, null, false);

                List<GitLabProject> gitLabProjects = gitLabClient.getGitLabProjects();

                Assertions.assertThat(gitLabProjects).isNotNull();
                Assertions.assertThat(gitLabProjects.size()).isEqualTo(4);

                List<String> actualProjectPaths = new ArrayList<>();
                for (var project : gitLabProjects)
                        actualProjectPaths.add(project.getFullPath());

                List<String> expectedProjectPaths = Arrays.asList(
                                "test-group/test-subgroup/test-project-1",
                                "test-group/test-subgroup/test-project-2",
                                "test-group/test-subgroup-2/test-project-3",
                                "test-group/test-subgroup-2/test-project-4");

                Assertions.assertThat(actualProjectPaths).isEqualTo(expectedProjectPaths);
        }

        @Test
        public void testGetGitLabProjectsWithTopics() throws IOException, URISyntaxException {
                String accessToken = "TEST_ACCESS_TOKEN";

                String result = resourceToString("/unit/gitlab-api-getgitlabprojects-topics-response.json",
                                StandardCharsets.UTF_8);

                stubFor(post(urlPathEqualTo("/api/graphql"))
                                .willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                                .withBody(result)));

                Config config = ConfigProvider.getConfig();

                List<String> topics = Arrays.asList("topic1");

                GitLabClient gitLabClient = new GitLabClient(accessToken, config, topics, false);

                List<GitLabProject> gitLabProjects = gitLabClient.getGitLabProjects();

                Assertions.assertThat(gitLabProjects).isNotNull();
                Assertions.assertThat(gitLabProjects.size()).isEqualTo(1);

                Assertions.assertThat("project/with/topic").isEqualTo(gitLabProjects.get(0).getFullPath());
        }

        @Test
        public void testJsonToList() {
                String accessToken = "my-access-token";
                GitLabClient client = new GitLabClient(accessToken);
                JSONArray jsonArray = new JSONArray();
                jsonArray.add("item1");
                jsonArray.add("item2");
                List<String> list = client.jsonToList(jsonArray);
                Assertions.assertThat(list).isNotNull();
                Assertions.assertThat(list.size()).isEqualTo(2); // assume 2 items are returned
                Assertions.assertThat("item1").isEqualTo(list.get(0));
                Assertions.assertThat("item2").isEqualTo(list.get(1));
        }

}