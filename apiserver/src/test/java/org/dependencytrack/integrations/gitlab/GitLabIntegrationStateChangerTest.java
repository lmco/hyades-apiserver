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

import alpine.model.IConfigProperty;
import alpine.model.Permission;
import alpine.model.Team;

import org.dependencytrack.PersistenceCapableTest;
import org.dependencytrack.auth.Permissions;

import org.junit.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dependencytrack.model.ConfigPropertyConstants.ACCESS_MANAGEMENT_ACL_ENABLED;
import static org.dependencytrack.model.ConfigPropertyConstants.GITLAB_API_KEY;
import static org.dependencytrack.model.ConfigPropertyConstants.GITLAB_ENABLED;
import static org.dependencytrack.model.ConfigPropertyConstants.GITLAB_SBOM_PUSH_ENABLED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GitLabIntegrationStateChangerTest extends PersistenceCapableTest {

    private GitLabIntegrationStateChanger stateChanger;

    @Before
    public void setUp() {
        stateChanger = new GitLabIntegrationStateChanger();
    }

    /**
     * Validates that the metadata is correctly defined.
     */
    @Test
    public void testIntegrationStateChangerMetadata() {
        Assertions.assertThat("GitLab Integration State Changer").isEqualTo(stateChanger.name());
        Assertions.assertThat("Executes GitLab integration enable and disable tasks").isEqualTo(stateChanger.description());
    }

    /**
     * Validates that the when integration is enabled the team is created.
     */
    @Test
    public void testEnable() {
        qm.createPermission(Permissions.Constants.BOM_UPLOAD, "upload BOMs");
        qm.createPermission(Permissions.Constants.VIEW_PORTFOLIO, "view portfolio");

        stateChanger.setQueryManager(qm);
        qm.createConfigProperty(
                ACCESS_MANAGEMENT_ACL_ENABLED.getGroupName(),
                ACCESS_MANAGEMENT_ACL_ENABLED.getPropertyName(),
                "false",
                IConfigProperty.PropertyType.BOOLEAN,
                null);

        qm.createConfigProperty(
                GITLAB_ENABLED.getGroupName(),
                GITLAB_ENABLED.getPropertyName(),
                "false",
                IConfigProperty.PropertyType.BOOLEAN,
                null);

        qm.createConfigProperty(
                GITLAB_API_KEY.getGroupName(),
                GITLAB_API_KEY.getPropertyName(),
                null,
                GITLAB_API_KEY.getPropertyType(),
                null);

        stateChanger.setState(true);
        List<Team> teamsList = qm.getTeams().getList(Team.class);

        Assertions.assertThat(teamsList.size().isEqualTo(1));
        Assertions.assertThat(teamsList.get(0).getName().isEqualTo("GitLab Users"));
    }

    /**
     * Validates that the when integration is disabled the teams is removed.
     */
    @Test
    public void testDisable() {
        qm.createPermission(Permissions.Constants.BOM_UPLOAD, "upload BOMs");
        qm.createPermission(Permissions.Constants.VIEW_PORTFOLIO, "view portfolio");

        stateChanger.setQueryManager(qm);
        qm.createConfigProperty(
                GITLAB_ENABLED.getGroupName(),
                GITLAB_ENABLED.getPropertyName(),
                "true",
                IConfigProperty.PropertyType.BOOLEAN,
                null);
        qm.createConfigProperty(
                ACCESS_MANAGEMENT_ACL_ENABLED.getGroupName(),
                ACCESS_MANAGEMENT_ACL_ENABLED.getPropertyName(),
                "false",
                IConfigProperty.PropertyType.BOOLEAN,
                null);
        qm.createConfigProperty(
                GITLAB_SBOM_PUSH_ENABLED.getGroupName(),
                GITLAB_SBOM_PUSH_ENABLED.getPropertyName(),
                "true",
                IConfigProperty.PropertyType.BOOLEAN,
                null);
        qm.createConfigProperty(
                GITLAB_API_KEY.getGroupName(),
                GITLAB_API_KEY.getPropertyName(),
                "test_api_key",
                GITLAB_API_KEY.getPropertyType(),
                null);

        // Create team to be removed
        stateChanger.setState(true);
        List<Team> teamsList = qm.getTeams().getList(Team.class);
        Assertions.assertThat(teamsList.size().isEqualTo(1));
        Assertions.assertThat(teamsList.get(0).getName().isEqualTo("GitLab Users"));

        // Disable the integration
        // and verify that the team is removed
        stateChanger.setState(false);
        teamsList = qm.getTeams().getList(Team.class);
        Assertions.assertThat(teamsList.size().isEqualTo(0));
    }

    @Test
    public void testPopulatePermissionsMap() {
        final var mockStateChanger = mock(GitLabIntegrationStateChanger.class);

        // Create Permission and add it to the map
        Permission permission = new Permission();
        permission.setName("VIEW_PORTFOLIO");
        Map<String, Permission> testPermissionsMap = new HashMap<String, Permission>();
        testPermissionsMap.put("VIEW_PORTFOLIO", permission);

        // Set query manager and permissions map for mockStateChanger
        mockStateChanger.setQueryManager(qm);
        when(mockStateChanger.getPermissionsMap()).thenReturn(testPermissionsMap);

        // Verify that the permission was added to the map
        Map<String, Permission> permissionsMap = mockStateChanger.getPermissionsMap();
        Assertions.assertThat(permissionsMap.size().isEqualTo(1));
        Assertions.assertThat(permissionsMap.containsKey("VIEW_PORTFOLIO").isTrue());
        Assertions.assertThat(permissionsMap.containsValue(permission).isTrue());
    }

    @Test
    public void testGetPermissionsMap() {
        stateChanger.setQueryManager(qm);

        // Test method call
        Map<String, Permission> permissionsMap = stateChanger.getPermissionsMap();

        // Verify expected state
        Assertions.assertThat(permissionsMap.size().isEqualTo(0));
    }

    @Test
    public void testGetPermissionsMapPopulated() {
        qm.createPermission("testPermission", "Test Permission");

        stateChanger.setQueryManager(qm);

        // Test method call
        stateChanger.getPermissionsMap();

        // Verify expected state
        Assertions.assertThat(stateChanger.getPermissionsMap().size().isEqualTo(1));
        Assertions.assertThat(stateChanger.getPermissionsMap().containsKey("testPermission").isTrue());
    }
}