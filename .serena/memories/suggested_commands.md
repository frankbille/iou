# Suggested commands
- `./gradlew spotlessCheck` - required before completing any task.
- `./gradlew spotlessApply` - run if `spotlessCheck` fails, then rerun `./gradlew spotlessCheck`.
- `./gradlew :server:test` - baseline backend verification after server changes.
- `./gradlew :composeApp:jsTest` - fastest frontend-only baseline check.
- `./gradlew :composeApp:compileDebugKotlinAndroid` - verify Android compile when touching expect/actual mobile code.
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` - verify iOS simulator compile when touching expect/actual mobile code.
- `rg PATTERN PATH` and `rg --files` - preferred fast search commands in this repo.
- Standard Darwin shell utilities are available: `git`, `ls`, `cd`, `find`, `sed`, `cat`, `open`.