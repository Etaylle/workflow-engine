package engine.settings;

import engine.shared.ApiProblem;
import engine.shared.ProjectPort;
import engine.shared.RunSettingsPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Step profiles and what each project chose; read by the run through RunSettingsPort. */
@Service
class SettingsService implements RunSettingsPort {

    static final BigDecimal MIN_LIMIT = BigDecimal.ONE;
    static final BigDecimal MAX_LIMIT = BigDecimal.valueOf(100_000);

    private final SettingsStore store;
    private final ProjectPort projects;

    SettingsService(SettingsStore store, ProjectPort projects) {
        this.store = store;
        this.projects = projects;
    }

    List<StepProfileView> profiles() {
        return store.profiles();
    }

    StepProfileView copy(UUID fromId, String rawName) {
        StepProfileView from = find(fromId);
        String name = StepProfileValidator.name(rawName);
        if (store.nameTaken(name, new UUID(0, 0))) {
            throw new ApiProblem(400, "Name in use", "A profile named \"" + name + "\" exists already.");
        }
        return find(store.copy(from.id(), name, "A copy of \"" + from.name() + "\"."));
    }

    StepProfileView update(UUID id, String rawName, List<Step> rawSteps) {
        StepProfileView profile = editable(id);
        String name = StepProfileValidator.name(rawName);
        List<Step> steps = StepProfileValidator.steps(rawSteps);
        if (store.nameTaken(name, profile.id())) {
            throw new ApiProblem(400, "Name in use", "A profile named \"" + name + "\" exists already.");
        }
        store.update(id, name, steps);
        return find(id);
    }

    void delete(UUID id) {
        store.delete(editable(id).id());
    }

    private StepProfileView find(UUID id) {
        return store.profile(id).orElseThrow(
                () -> new ApiProblem(404, "Profile not found", "No step profile " + id));
    }

    private StepProfileView editable(UUID id) {
        StepProfileView profile = find(id);
        if (profile.seeded()) {
            throw new ApiProblem(409, "Profile is read-only", "The profile \"" + profile.name()
                    + "\" is part of the engine. Copy it and edit the copy.");
        }
        return profile;
    }

    ProjectSettingsView projectSettings(UUID projectId) {
        requireProject(projectId);
        SettingsStore.Chosen chosen = store.chosen(projectId);
        StepProfileView profile = chosen.profileId() == null ? defaultProfile()
                : store.profile(chosen.profileId()).orElseGet(this::defaultProfile);
        boolean ownProfile = chosen.profileId() != null && profile.id().equals(chosen.profileId());
        return new ProjectSettingsView(profile.id(), profile.name(), !ownProfile, chosen.costLimitUsd());
    }

    ProjectSettingsView updateProjectSettings(UUID projectId, UUID profileId, BigDecimal limit) {
        requireProject(projectId);
        if (profileId != null) {
            find(profileId);
        }
        if (limit != null && (limit.compareTo(MIN_LIMIT) < 0 || limit.compareTo(MAX_LIMIT) > 0)) {
            throw new ApiProblem(400, "Cost limit out of range",
                    "Choose a cost limit from USD " + MIN_LIMIT + " to USD " + MAX_LIMIT
                            + " per run, or none for a run without a limit.");
        }
        store.choose(projectId, new SettingsStore.Chosen(profileId, limit));
        return projectSettings(projectId);
    }

    private StepProfileView defaultProfile() {
        return find(store.defaultProfileId().orElseThrow(
                () -> new IllegalStateException("No default step profile is seeded")));
    }

    private void requireProject(UUID projectId) {
        projects.statusOf(projectId).orElseThrow(
                () -> new ApiProblem(404, "Project not found", "No project " + projectId));
    }

    @Override
    public RunSettings forProject(UUID projectId) {
        SettingsStore.Chosen chosen = store.chosen(projectId);
        StepProfileView profile = Optional.ofNullable(chosen.profileId()).flatMap(store::profile)
                .orElseGet(this::defaultProfile);
        return new RunSettings(profile.name(), profile.steps(), Optional.ofNullable(chosen.costLimitUsd()));
    }
}
