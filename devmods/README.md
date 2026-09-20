# Development runtime mods

Place external mod/coremod jars used by the Gradle client and server launches in this directory.
The jars themselves are intentionally ignored by Git and are loaded by `dependencies.gradle`.

MC Heli's UniMixins integration requires this runtime file:

```text
+unimixins-all-1.7.10-0.3.1.jar
```

The build convention supplies the matching UniMixins compile-time API and annotation processor.
Keeping the runtime jar here makes its FML coremod available to the legacy 1.7.10 development
launch without adding another runtime-mod directory. Do not also add UniMixins under `libs/`,
`mods/`, or `run/mods/`; duplicate copies can bootstrap Mixin twice.
