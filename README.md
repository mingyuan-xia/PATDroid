# PATDroid [![Build Status](https://travis-ci.org/mingyuan-xia/PATDroid.svg?branch=master)](https://travis-ci.org/mingyuan-xia/PATDroid) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.mxia/patdroid/badge.svg)](https://maven-badges.herokuapp.com/maven-central/me.mxia/patdroid)
<img align="right" src="img/icon-small.png" />
PATDroid is a collection of tools and data structures for analyzing Android applications and the system itself. We intend to build it as a common base for developing novel mobile software debugging, refactoring, reverse engineering tools.

```groovy
dependencies {
    compile group: 'mxia.me', name: 'patdroid', version: '1.0.0'
}
```
The `master` branch is the nightly dev branch, which could diverge greatly from the maven artifacts.

## Packages
Here is a one-sentence description for each package. Find the detailed usage tutorials on our wiki by clicking on the package name to redirect to their wiki pages. Most public APIs are Java-doced. PATDroid requires Java6+. It goes well with Oracle/OpenJDK 1.6, 1.7, Dalvik (Yes, you can run it on a smartphone). Gradle (wrapper) is the default build system. You can import the project to IntelliJ IDEA (File->Import from Gradle Project) and Eclipse (similar).

* [`patdroid.core`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-core): provide abstractions for methods, classes, fields, and primitive Java type values
* [`patdroid.permission`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-permission): specify what Android permissions are needed for every Android APIs
* [`patdroid.fs`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-fs): an emulated and simplified Android file system
* [`patdroid.dalvik`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-dalvik): Android Dalvik JVM instructions and representations
* [`patdroid.smali`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-smali): using [SMALI](https://github.com/JesusFreke/smali) to extract classes, methods, fields and instructions from an APK

Closely related functionality:
* ~~`patdroid.dex2jar`~~: using [dex2jar](https://github.com/pxb1988/dex2jar) to extract classes, methods, fields and instructions from an APK. This has been deprecated and removed.
* Layout XMLs and manifest file, please refer to [apktool](https://ibotpeaches.github.io/Apktool/) and various AXML parsers exist for different programming languages.
* Taint sources and sinks: FlowDroid provides a list of [sources and sinks for taint analysis](https://github.com/secure-software-engineering/soot-infoflow-android/blob/develop/SourcesAndSinks.txt) that we cross referenced.
* Soot: my tribute to [Sable's Soot](http://sable.github.io/soot/) and the happy seminar time at [McGill McConnell 2rd floor](https://www.mcgill.ca/maps/mcconnell-engineering-building). Soot provides a disassembler similar to smali, and a lot of high-level program analysis constructs and tasks, such as Call Graph. Also [FlowDroid](https://github.com/secure-software-engineering/soot-infoflow-android) provides a nice and complete flow analysis.


## History and Philosophy
PATDroid was part of [AppAudit](http://appaudit.io), which is a tool that simulates the execution of app code and checks if it leaks sensitive user data.
You can find out more details from our [S&P'15 paper](http://www.ieee-security.org/TC/SP2015/papers-archived/6949a899.pdf).
We make part of AppAudit public to be useful to researchers and developers.
Overall, we try to make the entire project

1. concise (with fewer abstractions as possible such that users wont feel like searching a needle in the ocean)
2. properly documented (javadoc, and wiki tutorial)
3. loosely coupled (packages trying to be self-contained)
4. efficient (graduate students need life with bf/gf not waiting for computers to complete analyses)
5. look like good code

If you want to contribute, make sure you follow these traditions and feel free to submit a pull request.
Note that quick-and-dirty patches require many efforts to make them ready, and thus take more time to merge.
I am always open to suggestions and willing to hear interesting projects that make use of PATDroid.
Right now, several exciting research projects across McGill University and Shanghai Jiao Tong University are using PATDroid. We will update links to them soon.

* Contact: [email](mailto:mxia@mxia.me), new issues, pull requests.
* PATDroid uses `Apache License 2.0`. If you would like to use PATDroid in academic publications, bibtex can be found [here](http://dl.acm.org/citation.cfm?id=2867539.2867691).
