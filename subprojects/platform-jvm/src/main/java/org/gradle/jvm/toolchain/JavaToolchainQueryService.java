/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.toolchain;

import org.gradle.jvm.toolchain.internal.JavaInstallationRegistryShared;

import java.text.MessageFormat;
import java.util.stream.Collectors;

public class JavaToolchainQueryService {

    private final JavaInstallationRegistryShared installations;

    public JavaToolchainQueryService(JavaInstallationRegistryShared installations) {
        this.installations = installations;
    }

    public String query() {
        // query local installations, fallback to download
        return installations.getAllInstallations().stream().map(i -> MessageFormat.format("* {0} ({1})", i.getName(), i.getPath())).collect(Collectors.joining("\n"));
    }

}
