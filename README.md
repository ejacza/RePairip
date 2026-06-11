<p align="center">
  <img src="preview/re.png" width="100" height="100" alt="RePairip logo"/>
</p>

<h1 align="center">RePairip</h1>

<p align="center">
  A Java reverse-engineering utility for analyzing and rebuilding Android APK/APKS packages that use Google Play PairIP protection.
</p>

<p align="center">
  <b>Current version:</b> 1.4.14 &nbsp;|&nbsp;
  <b>Language:</b> Java 17 &nbsp;|&nbsp;
  <b>Build:</b> Gradle + Shadow Jar
</p>


## Overview

RePairip is a command-line tool that works with PairIP-protected Android packages. It can merge split `.apks` packages, patch PairIP-related dex code, clean split metadata from the manifest, rebuild the APK, and optionally apply a static translation JSON to restore dumped PairIP values

## Screenshots

| App UI | Dump Style |
| --- | --- |
| ![App UI](preview/a1.jpg) | ![Dump Style](preview/a2.jpg) |


### Full Pipeline

```text
// RePairip high-level flow
//
//                 input
//                   |
//        +----------+----------+
//        |                     |
//       .apk                  .apks
//        |                     |
//        |              +------v------+
//        |              |  Extract    |
//        |              |  Splits     |
//        |              +------+------+
//        |                     |
//        |              +------v------+
//        +------------->|  Merge APK  |
//                       +------+------+
//                              |
//                    +---------v----------+
//                    | Patch Manifest     |
//                    | - split metadata   |
//                    | - license entries  |
//                    +---------+----------+
//                              |
//                  +-----------v------------+
//                  | Translation provided?  |
//                  +-----------+------------+
//                       yes    |     no
//                              |
//        +---------------------+--------------------+
//        |                                          |
// +------v------+                           +-------v-------+
// | Rewrite     |                           | Logging Patch |
// | PairIP dex  |                           | + Dex Patch   |
// +------+------+                           +-------+-------+
//        |                                          |
// +------v------+                           +-------v-------+
// | Add restore |                           | Patch CRC32   |
// | dex/assets  |                           | if available  |
// +------+------+                           +-------+-------+
//        |                                          |
//        +---------------------+--------------------+
//                              |
//                       +------v------+
//                       | Build APK   |
//                       +------+------+
//                              |
//                           output
```

### Dynamic Dump / Logging Patch

```text
// Before
//
//             app start
//                |
//        +-------v--------+
//        | StartupLauncher |
//        | launch()        |
//        +-------+--------+
//                |
//        +-------v--------+
//        | VMRunner.invoke |
//        +-------+--------+
//                |
//              return
//
//
// After
//
//             app start
//                |
//        +-------v--------+
//        | StartupLauncher |
//        | launch()        |
//        +-------+--------+
//                |
//        +-------v--------+
//        | VMRunner.invoke |
//        +-------+--------+
//                |
//        +-------v--------+
//        | pairip()        |
//        | dump/log values |
//        +-------+--------+
//                |
//              return
```

### Static Translation Patch

```text
// Before
//
//          PairIP classes
//               |
//     +---------v----------+
//     | Obfuscated fields  |
//     | VM/runtime restore |
//     +---------+----------+
//               |
//        runtime lookup
//               |
//             return
//
//
// After
//
//          pairip.json
//               |
//     +---------v----------+
//     | TranslationData    |
//     | class -> fields    |
//     +---------+----------+
//               |
//     +---------v----------+
//     | TranslationRewriter|
//     | restore statically |
//     +---------+----------+
//               |
//     +---------v----------+
//     | Clean PairIP flow  |
//     | remove runtime lib |
//     +---------+----------+
//               |
//          translated APK
```

### Manifest Patch

```text
// AndroidManifest.xml cleanup
//
//     manifest/application
//              |
//   +----------v-----------+
//   | Remove split flags   |
//   | requiredSplitTypes   |
//   | splitTypes           |
//   | isSplitRequired      |
//   | isFeatureSplit       |
//   +----------+-----------+
//              |
//   +----------v-----------+
//   | Remove Play metadata |
//   | vending splits       |
//   | stamp source/type    |
//   +----------+-----------+
//              |
//   +----------v-----------+
//   | Remove PairIP license|
//   | activity/provider    |
//   | CHECK_LICENSE perm   |
//   +----------+-----------+
//              |
//       patched manifest
```

### Signature / Integrity Patch

```text
// Target methods
//
//   verifySignatureMatches()
//   verifyIntegrity()
//
//
// Before:
//
//       entry
//         |
//   +-----v------+
//   | original   |
//   | validation |
//   +-----+------+
//         |
//      result
//
//
// After:
//
//       entry
//         |
//   +-----v------+
//   | fixed safe |
//   | return     |
//   +-----+------+
//         |
//   true / void / null
```


## Project Structure

```text
RePairip/
├── antik/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/antik/
│       │   ├── Main.java
│       │   ├── AntikUtils.java
│       │   ├── DexPatcher/
│       │   │   ├── DexPatcher.java
│       │   │   ├── MethodT/
│       │   │   ├── PairipMethodMake/
│       │   │   └── Translation/
│       │   ├── crc32/
│       │   ├── manifest/
│       │   ├── ui/
│       │   └── utils/
│       └── resources/
│           ├── log.dex
│           ├── restoreMethod.dex
│           └── info.txt
├── gradle/
├── preview/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Dependencies

| Dependency | Use |
| --- | --- |
| `org.smali:dexlib2` | Read and write dex classes/methods/instructions |
| `com.github.REAndroid:ARSCLib` | Load, merge, patch, and write APK/APKS modules |
| `com.google.guava:guava` | Utility dependency |
| `com.gradleup.shadow` | Build the runnable fat jar |
---

## License

This project is distributed under the Apache License 2.0.

```text
Apache License
Version 2.0, January 2004
https://www.apache.org/licenses/
```

Copyright (C) 2026 HighCapable
