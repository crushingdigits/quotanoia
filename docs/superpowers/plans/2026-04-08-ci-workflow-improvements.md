# CI Workflow Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deduplicate CI setup steps via composite action, upgrade to setup-java@v5 with temurin, broaden PR triggers, and enable Gradle dependency caching with a weekly cache refresh.

**Architecture:** A local composite action (`.github/actions/setup-build-env/action.yml`) bundles Java + Gradle setup. All workflow jobs reference it instead of inline steps. A scheduled workflow keeps the cache warm.

**Tech Stack:** GitHub Actions, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `.github/actions/setup-build-env/action.yml` | Create | Composite action: setup-java@v5 (temurin, JDK 17) + setup-gradle@v4 |
| `.github/workflows/build.yml` | Modify | Update triggers + replace setup steps in all 5 jobs |
| `.github/workflows/update-screenshots.yml` | Modify | Replace setup steps with composite action |
| `.github/workflows/refresh-gradle-cache.yml` | Create | Weekly scheduled workflow to keep Gradle cache alive |

---

### Task 1: Create the composite action

**Files:**
- Create: `.github/actions/setup-build-env/action.yml`

- [ ] **Step 1: Create the composite action file**

```yaml
name: Setup Build Environment
description: Sets up JDK 17 (Temurin) and Gradle with dependency caching

runs:
  using: "composite"
  steps:
    - uses: actions/setup-java@v5
      with:
        distribution: temurin
        java-version: '17'
    - uses: gradle/actions/setup-gradle@v4
```

- [ ] **Step 2: Validate YAML syntax**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/actions/setup-build-env/action.yml'))"`
Expected: No output (valid YAML)

- [ ] **Step 3: Commit**

```bash
git add .github/actions/setup-build-env/action.yml
git commit -m "ci: add composite action for build environment setup"
```

---

### Task 2: Update `build.yml` triggers and replace setup steps

**Files:**
- Modify: `.github/workflows/build.yml`

- [ ] **Step 1: Update the trigger block**

Change lines 3-7 from:

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

This removes the branch filter on `pull_request`, so the workflow runs on all PRs.

- [ ] **Step 2: Replace setup steps in the `detekt` job**

Replace these steps (lines 22-26):

```yaml
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'
      - uses: gradle/actions/setup-gradle@v4
```

With:

```yaml
      - uses: ./.github/actions/setup-build-env
```

- [ ] **Step 3: Replace setup steps in the `lint` job**

Replace these steps (lines 40-44):

```yaml
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'
      - uses: gradle/actions/setup-gradle@v4
```

With:

```yaml
      - uses: ./.github/actions/setup-build-env
```

- [ ] **Step 4: Replace setup steps in the `unit-tests` job**

Replace these steps (lines 58-62):

```yaml
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'
      - uses: gradle/actions/setup-gradle@v4
```

With:

```yaml
      - uses: ./.github/actions/setup-build-env
```

- [ ] **Step 5: Replace setup steps in the `screenshots` job**

Replace these steps (lines 78-82):

```yaml
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'
      - uses: gradle/actions/setup-gradle@v4
```

With:

```yaml
      - uses: ./.github/actions/setup-build-env
```

Note: The `actions/checkout@v4` step with `lfs: true` stays as-is above this — only the java/gradle steps are replaced.

- [ ] **Step 6: Replace setup steps in the `build` job**

Replace these steps (lines 98-102):

```yaml
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'
      - uses: gradle/actions/setup-gradle@v4
```

With:

```yaml
      - uses: ./.github/actions/setup-build-env
```

- [ ] **Step 7: Validate the final `build.yml`**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))"`
Expected: No output (valid YAML)

Verify no remaining references to `setup-java` or `zulu`:

Run: `grep -n 'setup-java\|zulu' .github/workflows/build.yml`
Expected: No output (no matches)

Verify the composite action is referenced in all 5 jobs:

Run: `grep -c 'setup-build-env' .github/workflows/build.yml`
Expected: `5`

- [ ] **Step 8: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: use composite action and broaden PR triggers in build.yml"
```

---

### Task 3: Update `update-screenshots.yml`

**Files:**
- Modify: `.github/workflows/update-screenshots.yml`

- [ ] **Step 1: Replace setup steps**

Replace these steps (lines 23-31):

```yaml
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'zulu'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
```

With:

```yaml
      - uses: ./.github/actions/setup-build-env
```

- [ ] **Step 2: Validate**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/update-screenshots.yml'))"`
Expected: No output (valid YAML)

Run: `grep -n 'setup-java\|zulu' .github/workflows/update-screenshots.yml`
Expected: No output (no matches)

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/update-screenshots.yml
git commit -m "ci: use composite action in update-screenshots workflow"
```

---

### Task 4: Create weekly cache refresh workflow

**Files:**
- Create: `.github/workflows/refresh-gradle-cache.yml`

- [ ] **Step 1: Create the workflow file**

```yaml
name: Refresh Gradle Cache

on:
  schedule:
    - cron: '0 6 * * 1'  # Every Monday at 06:00 UTC
  workflow_dispatch:

permissions:
  contents: read

jobs:
  refresh-cache:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/setup-build-env
      - run: ./gradlew assembleDebug
```

- [ ] **Step 2: Validate YAML syntax**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/refresh-gradle-cache.yml'))"`
Expected: No output (valid YAML)

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/refresh-gradle-cache.yml
git commit -m "ci: add weekly Gradle cache refresh workflow"
```
