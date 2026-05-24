# Security Dependency Scan

Date: `2026-05-23`

## Command

The reproducible workspace run is:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% org.owasp:dependency-check-maven:12.1.0:check -DskipTests -Denforcer.skip=true -Dformats=HTML,JSON -DautoUpdate=false -DknownExploitedEnabled=false -DassemblyAnalyzerEnabled=false -DossindexAnalyzerEnabled=false -DossIndexAnalyzerEnabled=false -DnodeAuditAnalyzerEnabled=false -DnodePackageAnalyzerEnabled=false
```

Artifacts:

- `EcoBookAiBackend/target/dependency-check-report.html`
- `EcoBookAiBackend/target/dependency-check-report.json`

## Remediation Applied

The dependency baseline was rebased before this report was accepted:

- Spring Boot parent `3.2.0 -> 3.5.14`
- Firebase Admin `9.2.0 -> 9.9.0`
- JJWT `0.12.3 -> 0.13.0`
- Testcontainers `1.19.3 -> 1.21.4`
- PostgreSQL driver `42.7.1 -> 42.7.11`
- Springdoc `2.5.0 -> 2.8.17`
- Rest Assured `5.3.2 -> 5.5.6`
- Lombok `1.18.44 -> 1.18.46`
- removed unused Spring Cloud / OpenFeign wiring from the backend graph
- aligned Surefire to `3.5.4`
- excluded unneeded Firebase OpenTelemetry incubator artifacts and pinned `grpc-context`

## Result Summary

Previous baseline before remediation:

- `25` vulnerable dependencies
- `131` findings

Current validated baseline:

- `13` vulnerable dependencies
- `41` findings

Remaining flagged artifacts:

1. `angus-activation-2.0.3`
2. `commons-lang3-3.17.0`
3. `grpc-core-1.76.3`
4. `grpc-protobuf-1.76.3`
5. `hibernate-validator-8.0.3.Final`
6. `kotlin-stdlib-1.9.25`
7. `log4j-api-2.24.3`
8. `micrometer-registry-prometheus-1.15.11`
9. `netty-transport-4.1.132.Final`
10. `protobuf-java-4.33.2`
11. `swagger-ui-5.32.2` bundled DOMPurify
12. `tomcat-embed-core-10.1.54`

## Dependency Sources

Residual findings now concentrate in a few upstream chains:

- Boot-managed runtime stack:
  - `tomcat-embed-core`
  - `hibernate-validator`
  - `log4j-api`
  - `micrometer-registry-prometheus`
  - `angus-activation` via `jaxb-runtime`
- Firebase / Google Cloud transitive stack:
  - `grpc-core`
  - `grpc-protobuf`
  - `protobuf-java`
  - `netty-transport`
- Documentation UI bundle:
  - `swagger-ui` shipping bundled `DOMPurify`
- Test/runtime utility edges:
  - `commons-lang3` through `rest-assured`
  - `kotlin-stdlib` through `okhttp`

## Risk Decision

`T228` is considered complete because:

1. the scan now runs reproducibly in the repo
2. the direct dependency baseline was materially upgraded and retested
3. the report delta was reduced from `25/131` to `13/41`
4. the remaining findings are concentrated in framework-managed or transitive chains that would require either upstream patched releases or compatibility work broader than this hardening round

Accepted residual risk for this checkpoint:

- keep the current Spring Boot `3.5.14` / Springdoc `2.8.17` / Firebase `9.9.0` baseline because it is the highest locally revalidated combination in this repository
- track future patched releases for Boot-managed Tomcat/Log4j/Hibernate Validator/Micrometer artifacts
- track Firebase / Google Cloud releases for the grpc, protobuf and netty chain
- treat the Swagger UI `DOMPurify` issue as documentation-surface risk rather than authenticated API-runtime risk for the MVP
- accept `commons-lang3` as test-scope only in this workspace

## Phase 10 Status

`T228` is closed with documented residual risk. Any later dependency refresh should start by rerunning the exact command above and diffing the generated JSON report against this baseline.
