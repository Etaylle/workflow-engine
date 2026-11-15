package engine.settings;

import engine.shared.RunSettingsPort.Step;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations listStepProfiles, copyStepProfile, updateStepProfile, deleteStepProfile, getProjectSettings, updateProjectSettings. */
@RestController
class SettingsController {

    record CopyRequest(UUID fromId, String name) {
    }

    record ProfileEdit(String name, List<Step> steps) {
    }

    record ProjectSettingsRequest(UUID stepProfileId, BigDecimal costLimitUsd) {
    }

    private final SettingsService service;

    SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping("/api/settings/step-profiles")
    List<StepProfileView> profiles() {
        return service.profiles();
    }

    @PostMapping("/api/settings/step-profiles")
    @ResponseStatus(HttpStatus.CREATED)
    StepProfileView copy(@RequestBody CopyRequest request) {
        return service.copy(request.fromId(), request.name());
    }

    @PutMapping("/api/settings/step-profiles/{profileId}")
    StepProfileView update(@PathVariable UUID profileId, @RequestBody ProfileEdit edit) {
        return service.update(profileId, edit.name(), edit.steps());
    }

    @DeleteMapping("/api/settings/step-profiles/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID profileId) {
        service.delete(profileId);
    }

    @GetMapping("/api/projects/{id}/settings")
    ProjectSettingsView projectSettings(@PathVariable UUID id) {
        return service.projectSettings(id);
    }

    @PutMapping("/api/projects/{id}/settings")
    ProjectSettingsView updateProjectSettings(@PathVariable UUID id,
            @RequestBody ProjectSettingsRequest request) {
        return service.updateProjectSettings(id, request.stepProfileId(), request.costLimitUsd());
    }
}
