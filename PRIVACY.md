# Privacy Policy

## Purpose

Vectras VM Android is an Android virtualization runtime. It may execute local virtual machines, manage VM metadata, expose VNC/X11 display paths, integrate Termux-compatible runtime components, and use native acceleration for deterministic low-level operations.

This document describes the repository-level privacy expectations for the open-source project. App distributors must review and adapt this policy before publishing builds to users.

## Data Processed Locally

Depending on enabled features and user actions, the app may process or store locally:

- VM names and configuration metadata;
- VM disk image paths, ISO paths and CD-ROM paths;
- QEMU parameters selected by the user;
- VNC/X11 display settings;
- runtime logs, crash diagnostics and benchmark results;
- imported files selected by the user;
- local bootstrap/runtime assets required to start the environment.

The project should treat VM images, disk paths and configuration files as user-controlled private data.

## Network Access

The app declares network access because virtualization, update, web, VNC/X11, support, runtime download or community features may require connectivity.

Network behavior must remain explicit and reviewable. Features that contact external services should document:

- destination or service category;
- purpose;
- data sent;
- whether the feature is optional;
- user-visible control or setting when applicable.

## Advertising, Analytics and Third-party SDKs

The Android manifest may include metadata for third-party services. Any distributor that enables advertising, analytics, crash reporting or remote configuration must provide a production privacy notice that identifies those services and their data handling.

Third-party SDK use must be documented in `THIRD_PARTY_NOTICES.md` and, when privacy-relevant, in this file or a distributor-specific privacy notice.

## Microphone and Media Permissions

The manifest declares microphone and media-related permissions for runtime features that may require audio input/output or media/file selection. These permissions must degrade safely when denied.

Permission usage is documented in `docs/PERMISSIONS_RATIONALE.md`.

## Local Files and VM Images

Users may import, create or reference local files such as:

- VM disk images;
- ISO images;
- firmware/BIOS files;
- screenshots or thumbnails;
- exported VM configuration.

The project must not upload these files unless a feature explicitly asks the user to do so and documents the destination.

## Logs and Diagnostics

Logs may include device information, runtime state, file paths, ABI details, VM identifiers, error traces or benchmark outputs. Logs should avoid exposing secrets, personal files, complete command lines with sensitive paths, or user data from guest systems.

## Security and Abuse Prevention

Security reports should follow `SECURITY.md`.

Privacy-sensitive changes include:

- adding a new SDK;
- adding a network endpoint;
- changing file import/export behavior;
- changing logging behavior;
- changing permission behavior;
- changing QEMU command construction;
- changing crash or telemetry reporting.

## Distributor Responsibility

This repository-level policy is not a substitute for a store-ready legal privacy policy. Anyone distributing builds must verify:

- exact third-party SDKs enabled in that build;
- regional legal requirements;
- whether ads, analytics or crash reporting are active;
- data retention and deletion controls;
- support contact and reporting process.
