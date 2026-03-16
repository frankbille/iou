# Style and conventions
- Follow existing Kotlin and Gradle patterns already present in the touched module.
- Prefer shared GraphQL read DTOs and balance derivation logic in `shared/` instead of duplicating schema-facing models.
- For backend behavior changes, update GraphQL SDL in `server/src/main/resources/graphql/`, implementation code, and integration tests together.
- Trust code and tests over `SPEC.md` for implemented behavior.
- Keep client presentation models in `composeApp/` separate from generated GraphQL response models.
- PR descriptions should explain why the change exists and why the implementation is structured that way, starting with `Background` and `Solution`.
- `.serena/project.yml` is versioned; keep `project_name` stable as `iou`, keep durable onboarding knowledge in project memories, and leave local-only overrides in `.serena/project.local.yml`.