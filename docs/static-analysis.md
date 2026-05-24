# Static Analysis Baseline

Date: `2026-05-23`

## Checkstyle

Baseline config:

- [EcoBookAiBackend/config/checkstyle/checkstyle.xml](../EcoBookAiBackend/config/checkstyle/checkstyle.xml)

Command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% org.apache.maven.plugins:maven-checkstyle-plugin:3.5.0:check -Dcheckstyle.config.location=config/checkstyle/checkstyle.xml -Dcheckstyle.consoleOutput=true
```

Result:

- `0` violations after import cleanup in the domain/security/service layer

Rules currently enforced:

- no tab characters
- newline at end of file
- no unused imports
- no redundant imports
- no star imports
- braces/empty-block sanity checks

## SpotBugs

Environment note:

- Running SpotBugs under JDK `26` fails because the analyzer stack used in this workspace does not understand host class-file major version `70`.
- Running the same analysis under JDK `17` works because the project classes were already compiled and SpotBugs no longer needs to inspect JDK 26 runtime classes.

Command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs -DskipTests -Denforcer.skip=true
```

Artifacts:

- `EcoBookAiBackend/target/spotbugsXml.xml`

Result summary:

- `0` priority-1 findings after hardening `AuditActionAspect`
- `191` priority-2 findings remain
- dominant pattern: `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`

Interpretation:

- The remaining warnings are overwhelmingly about mutable DTO/model collections exposed through Lombok-generated getters, setters and builders.
- They do not currently indicate a proven runtime exploit or crash path, but they are still useful refactoring debt if the DTO layer is made immutable in the future.
- One missing-class notice remains for `java.lang.MatchException` because the analyzer is running on JDK `17`; it did not block the report.

## Phase 10 Status

This baseline is sufficient to treat `T227` as closed from a "critical issue addressed" standpoint:

- style baseline is reproducible and green
- the only high-priority SpotBugs issue was fixed
- remaining findings are documented, medium-severity mutability warnings rather than blocking runtime defects
