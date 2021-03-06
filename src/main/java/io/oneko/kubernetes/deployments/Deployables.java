package io.oneko.kubernetes.deployments;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import io.oneko.automations.LifetimeBehaviour;
import io.oneko.deployable.DeployableConfigurationTemplates;
import io.oneko.deployable.DeploymentBehaviour;
import io.oneko.docker.DockerRegistry;
import io.oneko.project.Project;
import io.oneko.project.ProjectConstants;
import io.oneko.project.ProjectVersion;
import io.oneko.projectmesh.MeshComponent;

public class Deployables {

	public static Deployable<ProjectVersion> of(ProjectVersion version) {
		return new Deployable<>() {

			@Override
			public ProjectVersion getEntity() {
				return version;
			}

			@Override
			public UUID getId() {
				return version.getId();
			}

			@Override
			public String getName() {
				return version.getName();
			}

			@Override
			public String getFullLabel() {
				return "Version " + version.getName() + " of project " + version.getProject().getName();
			}

			@Override
			public DockerRegistry getDockerRegistry() {
				return version.getProject().getDockerRegistry();
			}

			public Project getRelatedProject() {
				return version.getProject();
			}

			public ProjectVersion getRelatedProjectVersion() {
				return version;
			}

			@Override
			public DeployableConfigurationTemplates getConfigurationTemplates() {
				return DeployableConfigurationTemplates.of(version.getCalculatedConfigurationTemplates());
			}

			@Override
			public boolean isOutdated() {
				return version.isOutdated();
			}

			@Override
			public List<String> getUrls() {
				return version.getUrls();
			}			@Override
			public void setOutdated(boolean outdated) {
				version.setOutdated(outdated);
			}

			@Override
			public String getDockerContentDigest() {
				return version.getDockerContentDigest();
			}

			@Override
			public Optional<LifetimeBehaviour> calculateEffectiveLifetimeBehaviour() {
				return version.getEffectiveLifetimeBehaviour();
			}			@Override
			public void setUrls(List<String> urls) {
				version.setUrls(urls);
			}

			@Override
			public DeploymentBehaviour getDeploymentBehaviour() {
				return version.getDeploymentBehaviour();
			}

			@Override
			public Map.Entry<String, String> getPrimaryLabel() {
				return Pair.of(ProjectConstants.TemplateVariablesNames.ONEKO_VERSION, version.getId().toString());
			}			@Override
			public void setDockerContentDigest(String dockerContentDigest) {
				version.setDockerContentDigest(dockerContentDigest);
			}

			@Override
			public DesiredState getDesiredState() {
				return version.getDesiredState();
			}







			@Override
			public void setDesiredState(DesiredState desiredState) {
				version.setDesiredState(desiredState);
			}
		};
	}

	public static Deployable<MeshComponent> of(MeshComponent component) {
		return new Deployable<>() {

			@Override
			public MeshComponent getEntity() {
				return component;
			}

			@Override
			public UUID getId() {
				return component.getId();
			}

			@Override
			public String getName() {
				return component.getName();
			}

			@Override
			public String getFullLabel() {
				return "Component " + component.getName() + " of project mesh " + component.getOwner().getName();
			}

			@Override
			public DockerRegistry getDockerRegistry() {
				return component.getProject().getDockerRegistry();
			}

			public Project getRelatedProject() {
				return component.getProject();
			}

			public ProjectVersion getRelatedProjectVersion() {
				return component.getProjectVersion();
			}

			@Override
			public DeployableConfigurationTemplates getConfigurationTemplates() {
				return DeployableConfigurationTemplates.of(component.getCalculatedConfigurationTemplates());
			}

			@Override
			public boolean isOutdated() {
				return component.isOutdated();
			}

			@Override
			public List<String> getUrls() {
				return component.getUrls();
			}			@Override
			public void setOutdated(boolean outdated) {
				component.setOutdated(outdated);
			}

			@Override
			public String getDockerContentDigest() {
				return component.getDockerContentDigest();
			}

			@Override
			public Optional<LifetimeBehaviour> calculateEffectiveLifetimeBehaviour() {
				return component.getOwner().getLifetimeBehaviour();
			}			@Override
			public void setUrls(List<String> urls) {
				component.setUrls(urls);
			}

			@Override
			public DeploymentBehaviour getDeploymentBehaviour() {
				return component.getOwner().getDeploymentBehaviour();
			}

			@Override
			public Map.Entry<String, String> getPrimaryLabel() {
				return Pair.of(ProjectConstants.TemplateVariablesNames.ONEKO_MESH_COMPONENT, component.getId().toString());
			}			@Override
			public void setDockerContentDigest(String dockerContentDigest) {
				component.setDockerContentDigest(dockerContentDigest);
			}

			@Override
			public DesiredState getDesiredState() {
				return component.getDesiredState();
			}







			@Override
			public void setDesiredState(DesiredState desiredState) {
				component.setDesiredState(desiredState);
			}
		};
	}
}
