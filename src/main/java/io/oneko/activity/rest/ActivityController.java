package io.oneko.activity.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.oneko.activity.ActivityLog;
import io.oneko.configuration.Controllers;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@Slf4j
@RequestMapping(ActivityController.PATH)
public class ActivityController {

	public static final String PATH = Controllers.ROOT_PATH + "/activity";

	private final ActivityLog activityLog;
	private final ActivityDTOFactory activityDTOFactory;

	public ActivityController(ActivityLog activityLog, ActivityDTOFactory activityDTOFactory) {
		this.activityLog = activityLog;
		this.activityDTOFactory = activityDTOFactory;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'DOER', 'VIEWER')")
	@GetMapping
	Flux<ActivityDTO> getAllActivities(@RequestParam(required = false, defaultValue = "0") int pageIndex, @RequestParam(required = false, defaultValue = "10") int pageSize) {
		return this.activityLog.getAllPaged(pageIndex, pageSize).map(this.activityDTOFactory::create);
	}

}
