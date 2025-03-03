/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.ecosystemappeng.crda.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.redhat.ecosystemappeng.crda.integration.Constants;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record GraphRequest(
    String pkgManager,
    List<String> providers,
    DependencyTree tree,
    Map<String, List<Issue>> issues,
    Map<String, Remediation> remediations,
    Map<String, PackageRef> recommendations,
    List<ProviderStatus> providerStatuses) {

  public GraphRequest {
    Objects.requireNonNull(pkgManager);
    Objects.requireNonNull(providers);
    Objects.requireNonNull(tree);

    if (!Constants.PKG_MANAGERS.contains(pkgManager)) {
      throw new IllegalArgumentException("Unsupported package manager: " + pkgManager);
    }
    List<String> invalidProviders =
        providers.stream()
            .filter(Predicate.not(Constants.PROVIDERS::contains))
            .collect(Collectors.toList());
    if (!invalidProviders.isEmpty()) {
      throw new IllegalArgumentException("Unsupported providers: " + invalidProviders);
    }

    if (issues != null) {
      issues = Collections.unmodifiableMap(issues);
    } else {
      issues = Collections.emptyMap();
    }
    if (remediations != null) {
      remediations = Collections.unmodifiableMap(remediations);
    } else {
      remediations = Collections.emptyMap();
    }
    if (recommendations != null) {
      recommendations = Collections.unmodifiableMap(recommendations);
    } else {
      recommendations = Collections.emptyMap();
    }
    if (providerStatuses != null) {
      providerStatuses = Collections.unmodifiableList(providerStatuses);
    } else {
      providerStatuses = Collections.emptyList();
    }
  }

  public static class Builder {

    String pkgManager;
    List<String> providers;
    DependencyTree tree;
    Map<String, List<Issue>> issues;
    Map<String, Remediation> remediations;
    Map<String, PackageRef> recommendations;
    List<ProviderStatus> providerStatuses;

    public Builder(String pkgManager, List<String> providers) {
      this.pkgManager = pkgManager;
      this.providers = providers;
    }

    public Builder(GraphRequest copy) {
      this.pkgManager = copy.pkgManager;
      this.providers = copy.providers;
      this.tree = copy.tree;

      if (copy.issues != null) {
        this.issues = new HashMap<>(copy.issues);
      }

      if (copy.remediations != null) {
        this.remediations = new HashMap<>(copy.remediations);
      }

      if (copy.recommendations != null) {
        this.recommendations = new HashMap<>(copy.recommendations);
      }

      if (copy.providerStatuses != null) {
        this.providerStatuses = new ArrayList<>(copy.providerStatuses);
      }
    }

    public Builder tree(DependencyTree tree) {
      this.tree = tree;
      return this;
    }

    public Builder issues(Map<String, List<Issue>> issues) {
      this.issues = issues;
      return this;
    }
    ;

    public Builder remediations(Map<String, Remediation> remediations) {
      this.remediations = remediations;
      return this;
    }

    public Builder recommendations(Map<String, PackageRef> recommendations) {
      this.recommendations = recommendations;
      return this;
    }

    public Builder providerStatuses(List<ProviderStatus> providerStatuses) {
      this.providerStatuses = providerStatuses;
      return this;
    }

    public GraphRequest build() {
      return new GraphRequest(
          pkgManager, providers, tree, issues, remediations, recommendations, providerStatuses);
    }
  }
}
