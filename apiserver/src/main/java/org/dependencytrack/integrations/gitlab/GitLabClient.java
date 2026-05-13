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

import org.dependencytrack.common.HttpClient;

import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.dependencytrack.common.ConfigKeys;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;

public class GitLabClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabClient.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String GRAPHQL_ENDPOINT = "/api/graphql";

    private final String accessToken;
    private final URI baseURL;
    private final Config config;
    private final List<String> topics;
    private final boolean includeArchived;

    public static final String PROJECT_PATH_CLAIM = "project_path";
    public static final String REF_PATH_CLAIM = "ref_path";
    public static final String REF_TYPE_CLAIM = "ref_type";
    public static final String USER_ACCESS_LEVEL_CLAIM = "user_access_level";

    public GitLabClient(final String accessToken) {
        this(accessToken, ConfigProvider.getConfig(), null, false);
    }

    public GitLabClient(final String accessToken, final List<String> topics, final boolean includeArchived) {
        this(accessToken, ConfigProvider.getConfig(), topics, includeArchived);
    }

    public GitLabClient(final String accessToken, final Config config, final List<String> topics,
            final boolean includeArchived) {
        this.accessToken = accessToken;
        this.baseURL = URI.create(config.getConfigValue(ConfigKeys.OIDC_ISSUER).getValue());
        this.config = config;
        this.includeArchived = includeArchived;
        this.topics = topics;
    }

    public List<GitLabProject> getGitLabProjects() throws IOException, URISyntaxException {
        List<GitLabProject> projects = new ArrayList<>();

        JSONObject variables = new JSONObject();
        JSONObject queryObject = new JSONObject();

        // Set the default values for the GraphQL query
        variables.put("includeTopics", false);
        variables.put("archived", includeArchived ? "INCLUDE" : "EXCLUDE");

        if (topics != null && !topics.isEmpty()) {
            variables.put("includeTopics", true);
            variables.put("topics", topics);
        }

        queryObject.put("query", IOUtils.resourceToString("/graphql/gitlab-projects.graphql", StandardCharsets.UTF_8));

        URIBuilder builder = new URIBuilder(baseURL.toString()).setPath(GRAPHQL_ENDPOINT);

        final var requestBuilder = HttpRequest.newBuilder()
                .uri(builder.build())
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json");

        while (true) {
            queryObject.put("variables", variables);

            StringEntity entity = new StringEntity(queryObject.toString(), StandardCharsets.UTF_8);
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(entity.toString()));

            try {
                HttpResponse<InputStream> response = HttpClient.INSTANCE.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
                int statusCode = response.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    LOGGER.warn("GitLab GraphQL query failed with status code: " + statusCode);
                    break;
                }

                JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
                JSONObject responseData = parser.parse(response.body(), JSONObject.class);

                // Check for GraphQL errors
                if (responseData.containsKey("errors")) {
                    LOGGER.warn("GitLab GraphQL query returned errors: " + responseData.get("errors"));
                    break;
                }

                JSONObject dataObject = (JSONObject) responseData.getOrDefault("data", new JSONObject());
                JSONObject projectsObject = (JSONObject) dataObject.getOrDefault("withoutTopics",
                        dataObject.getOrDefault("withTopics", new JSONObject()));
                JSONArray nodes = (JSONArray) projectsObject.getOrDefault("nodes", new JSONArray());

                for (Object nodeObject : nodes) {
                    JSONObject node = (JSONObject) nodeObject;
                    projects.add(GitLabProject.parse(node.toJSONString()));
                }

                JSONObject pageInfo = (JSONObject) projectsObject.getOrDefault("pageInfo", new JSONObject());

                if (!(boolean) pageInfo.get("hasNextPage"))
                    break;

                variables.put("cursor", pageInfo.getAsString("endCursor"));
            } catch(Exception e) {
                System.out.println(e);
            }
        }

        return projects;
    }


    // JSONArray to ArrayList simple converter
    public ArrayList<String> jsonToList(final JSONArray jsonArray) {
        ArrayList<String> list = new ArrayList<>();

        for (Object o : jsonArray != null ? jsonArray : Collections.emptyList())
            list.add(o.toString());

        return list;
    }

    public Config getConfig() {
        return config;
    }
}