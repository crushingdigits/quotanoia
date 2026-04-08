# CI Workflow Improvements Design

## Context

PR #7 introduced the GitHub Actions CI pipeline with 5 jobs in `build.yml` (detekt, lint, unit-tests, screenshots, build) and a separate `update-screenshots.yml`. The current setup has several issues:

1. **Repeated setup steps** — `actions/setup-java@v4` + `gradle/actions/setup-gradle@v4` duplicated across all 6 job definitions
2. **Outdated action version** — Using `setup-java@v4` with `zulu` distribution instead of latest `v5` with `temurin`
3. **Restrictive trigger** — Workflow only runs on PRs targeting `main`, missing PRs to other branches
4. **No Gradle caching** — Dependencies downloaded fresh on every run

## Changes

### 1. Composite Action: `.github/actions/setup-build-env/action.yml`

A local composite action that bundles Java and Gradle setup into a single reusable step:

- `actions/setup-java@v5` with `temurin` distribution, Java 17
- `gradle/actions/setup-gradle@v4` with default caching enabled

No inputs needed. Each job will replace its inline setup-java + setup-gradle steps with:
```yaml
- uses: ./.github/actions/setup-build-env
```

### 2. Trigger Changes in `build.yml`

From:
```yaml
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
```

To:
```yaml
on:
  push:
    branches: [ main ]
  pull_request:
```

This runs the workflow on all PRs regardless of target branch, plus pushes to main.

### 3. Gradle Dependency Caching

`gradle/actions/setup-gradle@v4` provides caching out of the box with no extra configuration:

- **What's cached:** Gradle distributions, dependencies, compiled build scripts, artifact transforms, local build cache
- **Where:** GitHub Actions Cache (10 GB per repo, 7-day eviction for unused entries)
- **Write policy:** Only jobs on `main` branch write to the cache. PR branches are read-only (restore from main's cache, don't write back)
- **Cache key:** OS + job ID + workflow + git SHA, with progressively less specific fallback restore keys

### 4. Weekly Cache Refresh: `.github/workflows/refresh-gradle-cache.yml`

A scheduled workflow running once a week to keep the Gradle cache alive (prevents 7-day eviction during inactive periods):

- Runs on `schedule` (weekly) and `workflow_dispatch` (manual trigger)
- Checks out `main`, uses the composite action, runs `./gradlew assembleDebug` to prime the full dependency tree
- Since this runs on `main`, it writes to the cache

### 5. Update `update-screenshots.yml`

Replace inline setup-java + setup-gradle with the composite action, matching the pattern in `build.yml`.

## Files Affected

| File | Change |
|------|--------|
| `.github/actions/setup-build-env/action.yml` | **New** — composite action |
| `.github/workflows/build.yml` | Update triggers + replace setup steps in all 5 jobs |
| `.github/workflows/update-screenshots.yml` | Replace setup steps |
| `.github/workflows/refresh-gradle-cache.yml` | **New** — weekly cache refresh |

## Non-Goals

- No changes to job logic, Gradle tasks, or artifact uploads
- No Gradle cache encryption (unnecessary for this project)
- No build scans configuration
- No changes to concurrency or permissions settings
