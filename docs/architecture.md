# Architecture

The Android app is organized by business domain, with supporting code grouped by responsibility.

```text
domain/     Business-focused areas and their UI, state, and models
data/       Persistence and repository implementations
platform/   Android platform integrations
shared/     Reusable UI, navigation, utilities, and theme
```

Only the `client` domain has its internal package structure initially. Other domains will be
expanded when their implementation begins.
