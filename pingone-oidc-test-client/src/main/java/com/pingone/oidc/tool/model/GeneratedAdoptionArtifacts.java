package com.pingone.oidc.tool.model;

public record GeneratedAdoptionArtifacts(
        String applicationType,
        boolean runnableInTemplate,
        String applicationYaml,
        String envVariables,
        String javaIntegrationNotes,
        String pingOneAdminSteps,
        String copyManifest,
        String adoptionGuide,
        String libraryDependency) {

    public GeneratedAdoptionArtifacts(
            String applicationYaml,
            String envVariables,
            String javaIntegrationNotes,
            String pingOneAdminSteps) {
        this(null, false, applicationYaml, envVariables, javaIntegrationNotes, pingOneAdminSteps, "", "", "");
    }

    public GeneratedAdoptionArtifacts(
            String applicationType,
            boolean runnableInTemplate,
            String applicationYaml,
            String envVariables,
            String javaIntegrationNotes,
            String pingOneAdminSteps,
            String copyManifest) {
        this(
                applicationType,
                runnableInTemplate,
                applicationYaml,
                envVariables,
                javaIntegrationNotes,
                pingOneAdminSteps,
                copyManifest,
                "",
                "");
    }
}
