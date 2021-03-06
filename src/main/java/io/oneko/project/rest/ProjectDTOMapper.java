package io.oneko.project.rest;

import static java.util.Optional.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;

import io.oneko.automations.LifetimeBehaviourDTOMapper;
import io.oneko.deployable.AggregatedDeploymentStatus;
import io.oneko.docker.DockerRegistry;
import io.oneko.kubernetes.deployments.DeployableStatus;
import io.oneko.kubernetes.deployments.Deployment;
import io.oneko.kubernetes.deployments.DeploymentDTO;
import io.oneko.kubernetes.deployments.DeploymentDTOs;
import io.oneko.kubernetes.deployments.DeploymentRepository;
import io.oneko.namespace.ImplicitNamespace;
import io.oneko.namespace.rest.NamespaceDTOMapper;
import io.oneko.project.Project;
import io.oneko.project.ProjectConstants;
import io.oneko.project.ProjectVersion;
import io.oneko.project.TemplateVariable;
import io.oneko.templates.rest.ConfigurationTemplateDTOMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProjectDTOMapper {
	private static final List<String> IMPLICIT_PROJECT_TEMPLATE_VARIABLES = Arrays.asList(
			ProjectConstants.TemplateVariablesNames.PROJECT_NAME,
			ProjectConstants.TemplateVariablesNames.ONEKO_PROJECT
	);

	private static final List<String> IMPLICIT_PROJECT_VERSION_TEMPLATE_VARIABLES = Arrays.asList(
			ProjectConstants.TemplateVariablesNames.VERSION_NAME,
			ProjectConstants.TemplateVariablesNames.SAFE_VERSION_NAME,
			ProjectConstants.TemplateVariablesNames.ONEKO_VERSION
	);

	private final ConfigurationTemplateDTOMapper templateDTOMapper;
	private final NamespaceDTOMapper namespaceDTOMapper;
	private final DeploymentRepository deploymentRepository;
	private final LifetimeBehaviourDTOMapper lifetimeBehaviourDTOMapper;

	public ProjectDTOMapper(ConfigurationTemplateDTOMapper templateDTOMapper, NamespaceDTOMapper namespaceDTOMapper, DeploymentRepository deploymentRepository, LifetimeBehaviourDTOMapper lifetimeBehaviourDTOMapper) {
		this.templateDTOMapper = templateDTOMapper;
		this.namespaceDTOMapper = namespaceDTOMapper;
		this.deploymentRepository = deploymentRepository;
		this.lifetimeBehaviourDTOMapper = lifetimeBehaviourDTOMapper;
	}

	public Mono<ProjectDTO> projectToDTO(Project project) {
		ProjectDTO dto = new ProjectDTO();
		dto.setUuid(project.getId());
		dto.setName(project.getName());
		dto.setDockerRegistryUUID(project.getDockerRegistryUuid());
		dto.setImageName(project.getImageName());
		dto.setNewVersionsDeploymentBehaviour(project.getNewVersionsDeploymentBehaviour());
		dto.setDefaultLifetimeBehaviour(project.getDefaultLifetimeBehaviour().map(lifetimeBehaviourDTOMapper::toLifetimeBehaviourDTO).orElse(null));
		dto.setDefaultConfigurationTemplates(project.getDefaultConfigurationTemplates().stream()
				.map(templateDTOMapper::toDTO)
				.collect(Collectors.toList()));
		dto.setTemplateVariables(toTemplateVariableDTOs(project.getTemplateVariables()));

		return projectVersionsToDTO(project.getVersions())
				.collectList()
				.map(versions -> {
					dto.setVersions(versions);
					final AggregatedDeploymentStatus aggregatedDeploymentStatus = aggregateDeploymentStatus(versions);
					dto.setStatus(aggregatedDeploymentStatus);
					return dto;
				});
	}

	private List<TemplateVariableDTO> toTemplateVariableDTOs(List<TemplateVariable> templateVariables) {
		return templateVariables.stream()
				.map(this::toTemplateVariableDTO)
				.collect(Collectors.toList());
	}

	private TemplateVariableDTO toTemplateVariableDTO(TemplateVariable templateVariable) {
		return TemplateVariableDTO.builder()
				.id(templateVariable.getId())
				.name(templateVariable.getName())
				.label(templateVariable.getLabel())
				.values(templateVariable.getValues())
				.useValues(templateVariable.isUseValues())
				.defaultValue(templateVariable.getDefaultValue())
				.showOnDashboard(templateVariable.isShowOnDashboard())
				.build();
	}

	private List<TemplateVariable> fromTemplateVariableDTOs(List<TemplateVariableDTO> templateVariables) {
		return templateVariables.stream()
				.map(this::fromTemplateVariableDTO)
				.collect(Collectors.toList());
	}

	private TemplateVariable fromTemplateVariableDTO(TemplateVariableDTO templateVariable) {
		return new TemplateVariable(MoreObjects.firstNonNull(templateVariable.getId(), UUID.randomUUID()),
				templateVariable.getName(),
				templateVariable.getLabel(),
				templateVariable.getValues(),
				templateVariable.isUseValues(),
				templateVariable.getDefaultValue(),
				templateVariable.isShowOnDashboard());
	}

	private AggregatedDeploymentStatus aggregateDeploymentStatus(Collection<ProjectVersionDTO> versionDTOs) {
		return DeploymentDTOs.aggregate(versionDTOs.stream()
				.map(ProjectVersionDTO::getDeployment)
				.collect(Collectors.toList()));
	}

	private Flux<ProjectVersionDTO> projectVersionsToDTO(Collection<ProjectVersion> versions) {
		return Flux.concat(versions.stream()
				.map(this::projectVersionToDTO)
				.collect(Collectors.toList()));
	}

	private Mono<ProjectVersionDTO> projectVersionToDTO(ProjectVersion version) {
		return deploymentRepository.findByDeployableId(version.getId())
				.map(deployment -> projectVersionToDTO(version, deployment))
				.switchIfEmpty(Mono.justOrEmpty(projectVersionToDTO(version, null)));
	}

	private ProjectVersionDTO projectVersionToDTO(ProjectVersion version, Deployment deployment) {
		ProjectVersionDTO dto = new ProjectVersionDTO();
		dto.setUuid(version.getUuid());
		dto.setName(version.getName());
		dto.setDeploymentBehaviour(version.getDeploymentBehaviour());
		dto.setAvailableTemplateVariables(toTemplateVariableDTOs(version.getProject().getTemplateVariables()));
		dto.setTemplateVariables(version.getTemplateVariables());
		dto.setDeployment(DeploymentDTO.create(version.getDeploymentBehaviour(), version.getId(), deployment));
		dto.setUrls(version.getUrls());
		dto.setOutdated(version.isOutdated());
		dto.setConfigurationTemplates(version.getConfigurationTemplates().stream()
				.map(templateDTOMapper::toDTO)
				.collect(Collectors.toList()));
		dto.setLifetimeBehaviour(version.getLifetimeBehaviour().map(lifetimeBehaviourDTOMapper::toLifetimeBehaviourDTO).orElse(null));
		dto.setNamespace(namespaceDTOMapper.namespaceToDTO(version.getNamespace()));
		dto.setImplicitNamespace(namespaceDTOMapper.namespaceToDTO(new ImplicitNamespace(version)));
		dto.setDesiredState(version.getDesiredState());
		dto.setImageUpdatedDate(version.getImageUpdatedDate());
		return dto;
	}

	public Mono<Project> updateProjectFromDTO(Project project, ProjectDTO projectDTO, DockerRegistry registry) {
		//id can not be changed
		project.setName(projectDTO.getName());
		project.setImageName(projectDTO.getImageName());
		project.assignToNewRegistry(registry);
		ofNullable(projectDTO.getNewVersionsDeploymentBehaviour()).ifPresent(project::setNewVersionsDeploymentBehaviour);
		project.setDefaultConfigurationTemplates(templateDTOMapper.updateFromDTOs(project.getDefaultConfigurationTemplates(), projectDTO.getDefaultConfigurationTemplates()));
		project.setDefaultLifetimeBehaviour(ofNullable(projectDTO.getDefaultLifetimeBehaviour()).map(lifetimeBehaviourDTOMapper::toLifetimeBehaviour).orElse(null));
		project.setTemplateVariables(fromTemplateVariableDTOs(projectDTO.getTemplateVariables()));

		return updateProjectVersionsFromDTO(project.getVersions(), projectDTO.getVersions())
				.collectList()
				.thenReturn(project);
	}

	private Flux<ProjectVersion> updateProjectVersionsFromDTO(Collection<ProjectVersion> versions, List<ProjectVersionDTO> versionDTOs) {
		final Map<UUID, ProjectVersionDTO> versionDTOsById = versionDTOs.stream().collect(Collectors.toMap(ProjectVersionDTO::getUuid, Function.identity()));
		return Flux.concat(versions.stream()
				.map(version -> updateProjectVersionFromDTO(version, versionDTOsById.get(version.getUuid())).thenReturn(version))
				.collect(Collectors.toList()));
	}

	private Mono<Void> updateProjectVersionFromDTO(ProjectVersion version, ProjectVersionDTO projectVersionDTO) {
		if (projectVersionDTO == null) {
			return Mono.empty();
		}
		ofNullable(projectVersionDTO.getDeploymentBehaviour()).ifPresent(version::setDeploymentBehaviour);

		final Map<String, String> templateVariables = new HashMap<>();
		for (Map.Entry<String, String> entry : projectVersionDTO.getTemplateVariables().entrySet()) {
			if (ListUtils.union(IMPLICIT_PROJECT_TEMPLATE_VARIABLES, IMPLICIT_PROJECT_VERSION_TEMPLATE_VARIABLES).contains(entry.getKey())) {
				continue;
			}
			templateVariables.put(entry.getKey(), entry.getValue());
		}
		version.setTemplateVariables(templateVariables);

		version.setConfigurationTemplates(templateDTOMapper.updateFromDTOs(version.getConfigurationTemplates(), projectVersionDTO.getConfigurationTemplates()));
		version.setLifetimeBehaviour(ofNullable(projectVersionDTO.getLifetimeBehaviour()).filter(dto -> dto.getDaysToLive() != -1).map(lifetimeBehaviourDTOMapper::toLifetimeBehaviour).orElse(null));
		return namespaceDTOMapper.updateNamespaceOfOwner(version, projectVersionDTO.getNamespace())
				.then(updateDeploymentStatusOfVersion(version));
	}

	private Mono<Void> updateDeploymentStatusOfVersion(ProjectVersion version) {
		return this.deploymentRepository.findByDeployableId(version.getId())
				.map(deployment -> {
					if (shouldVersionBeMarkedAsOutdated(version, deployment)) {
						version.setOutdated(true);
					}
					return version;
				}).then();
	}

	private boolean shouldVersionBeMarkedAsOutdated(ProjectVersion version, Deployment deployment) {
		if (deployment == null || deployment.getStatus() == DeployableStatus.NotScheduled) {
			return false;
		}
		if (CollectionUtils.containsAny(version.getProject().getDirtyProperties(), Arrays.asList("defaultConfigurationTemplates", "imageName", "defaultTemplateVariables", "dockerRegistry"))) {
			return true;
		}
		return CollectionUtils.containsAny(version.getDirtyProperties(), Arrays.asList("configurationTemplates", "templateVariables"));
	}
}
