# Optional HDMI startup helper

This Apache-2.0 component is the upstream Karat One Touch Play helper. It is a separate platform app, with a boot receiver that makes a single bounded HDMI announcement on a compatible custom Android 13 image. It can wake the display and select the Stick's input.

The component requires API 33 and the platform-granted HDMI_CEC permission. Our Fire OS baseline is API 30, and Screenroom uses a local app signing key. The regular Screenroom build excludes this helper. The copied upstream builder targets the platform image and requires the corresponding SDK and signing material.

We retained the upstream source, builder, and manifest as a self-contained reference. Read the upstream notes for the one-device validation limits. [Upstream helper](https://github.com/jrobdog/lineage-karat/tree/4b443865baa8768675910f81a2c80c3fa858872c/apps/KaratCecStartup)
