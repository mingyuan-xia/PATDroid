# PATDroid [![Build Status](https://travis-ci.org/mingyuan-xia/PATDroid.svg?branch=master)](https://travis-ci.org/mingyuan-xia/PATDroid)
<img align="right" src="img/icon-small.png" />
PATDroid is a collection of tools and data structures for analyzing Android applications and the system itself. We intend to build it as a common base for developing novel mobile software debugging, refactoring, reliability/security tools. We also collect various resources, links, related papers and tips for various innovative Android program analysis tasks.

## Changelog
* Now the project is in alpha stage. We are now working on several aspects to hit our first release
  * Documentation, tutorial, IDE support
  * Maven central support
  * flexible API level loading
  * let us know what you are expecting :-)

## Packages
Here is a one-sentence description for each package. Find the detailed usage tutorials on our wiki by clicking on the package name. Most public APIs are Java-doced. PATDroid requires Java6+. It goes well with Oracle/OpenJDK 1.6, 1.7, Dalvik (Yes, you can run it on a smartphone). We provide gradle, Intellij IDEA and Eclipse support for the project.

* [`patdroid.core`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-core): provide abstractions for methods, classes, fields, and primitive Java type values
* [`patdroid.permission`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-permission): specify what Android permissions are needed for every Android APIs
* [`patdroid.fs`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-fs): an emulated and simplified Android file system
* [`patdroid.dalvik`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-dalvik): Android Dalvik JVM instructions and representations
* [`patdroid.smali`](https://github.com/mingyuan-xia/PATDroid/wiki/package:-smali): using [SMALI](https://github.com/JesusFreke/smali) to extract classes, methods, fields and instructions from an APK 

According to our blueprint, we plan to release the following components one by one in the near future:
* ~~`patdroid.dex2jar`: using [dex2jar](https://github.com/pxb1988/dex2jar) to extract classes, methods, fields and instructions from an APK~~
* `patdroid.manifest`: the model for AndroidManifest.xml and Android components such as activity, service, broadcast receivers
* `patdroid.taint`: sources, sinks and taint propagation support for taint analysis
* `patdroid.lifecycle`: modelling the life cycle methods for Android components (Activity, Service, etc)
* `patdroid.layout`: parsing and manipulating layout.xml
* `patdroid.soot`: my tribute to [Sable's Soot](http://sable.github.io/soot/). I learned a lot from attending Sable's seminars held at [McGill McConnell 2rd floor](https://www.mcgill.ca/maps/mcconnell-engineering-building)

## Using PATDroid
PATDroid uses `Apache License 2.0`. The is the bib entry in case you want to use PATDroid in academic publications:
```bibtex
@inproceedings{appaudit,
 author = {Mingyuan Xia and Lu Gong and Yuanhao Lyu and Zhengwei Qi and Xue Liu},
 title = {Effective Real-time Android Application Auditing},
 booktitle = {Proceedings of the 2015 IEEE Symposium on Security and Privacy},
 series = {SP '15},
 year = {2015},
 publisher = {IEEE Computer Society},
} 
```

## History and Philosophy
PATDroid was part of [AppAudit](http://appaudit.io), which is a security tool that checks if an Android app leaks sensitive user data.
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
