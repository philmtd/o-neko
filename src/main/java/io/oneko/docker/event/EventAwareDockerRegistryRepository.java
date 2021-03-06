package io.oneko.docker.event;

import io.oneko.docker.DockerRegistry;
import io.oneko.docker.DockerRegistryRepository;
import io.oneko.event.EventDispatcher;
import reactor.core.publisher.Mono;

public abstract class EventAwareDockerRegistryRepository implements DockerRegistryRepository {

	private final EventDispatcher eventDispatcher;

	protected EventAwareDockerRegistryRepository(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	@Override
	public Mono<DockerRegistry> add(DockerRegistry dockerRegistry) {
		if (dockerRegistry.isDirty()) {
			Mono<DockerRegistry> dockerRegistryMono = addInternally(dockerRegistry);
			return this.eventDispatcher.createAndDispatchEvent(dockerRegistryMono, (d, t) -> new DockerRegistrySavedEvent(dockerRegistry, t));
		} else {
			return Mono.just(dockerRegistry);
		}
	}

	protected abstract Mono<DockerRegistry> addInternally(DockerRegistry dockerRegistry);

	@Override
	public Mono<Void> remove(DockerRegistry dockerRegistry) {
		Mono<Void> voidMono = removeInternally(dockerRegistry);
		return this.eventDispatcher.createAndDispatchEvent(voidMono, (v, trigger) -> new DockerRegistryDeletedEvent(dockerRegistry, trigger));
	}

	protected abstract Mono<Void> removeInternally(DockerRegistry dockerRegistry);
}
