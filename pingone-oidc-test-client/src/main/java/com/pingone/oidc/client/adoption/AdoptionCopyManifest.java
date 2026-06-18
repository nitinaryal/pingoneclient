package com.pingone.oidc.client.adoption;

import java.util.List;

/**
 * Describes which packages and steps adopters should copy into their own codebase.
 */
public record AdoptionCopyManifest(String title, List<String> packages, List<String> mavenDependencies, List<String> setupSteps) {

    public String format() {
        StringBuilder builder = new StringBuilder();
        builder.append("=== ").append(title).append(" ===\n\n");
        builder.append("Copy these packages into your project:\n");
        packages.forEach(pkg -> builder.append("  - ").append(pkg).append('\n'));
        if (!mavenDependencies.isEmpty()) {
            builder.append("\nMaven dependencies (usually already present for Spring Boot OAuth2):\n");
            mavenDependencies.forEach(dep -> builder.append("  - ").append(dep).append('\n'));
        }
        builder.append("\nSetup steps:\n");
        for (int i = 0; i < setupSteps.size(); i++) {
            builder.append("  ").append(i + 1).append(". ").append(setupSteps.get(i)).append('\n');
        }
        return builder.toString().stripTrailing();
    }
}
