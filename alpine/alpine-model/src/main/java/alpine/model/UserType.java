/*
 * This file is part of Alpine.
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
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.model;

import org.apache.commons.lang3.EnumUtils;

public enum UserType {
    MANAGED(ManagedUser.class),
    LDAP(LdapUser.class),
    OIDC(OidcUser.class);

    private final Class<? extends User> userClass;

    UserType(Class<? extends User> userClass) {
        this.userClass = userClass;
    }

    public static UserType fromString(String type) {
        return EnumUtils.getEnumIgnoreCase(UserType.class, type);
    }

    public Class<? extends User> getUserClass() {
        return userClass;
    }

    public String toString() {
        return this.name().toLowerCase();
    }

}
