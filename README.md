# MCH-mocmaster


# NEW! (experimental):

# How do I use this with assets?:

A. Find the repo's directory build\run\mods
B. Put your assets into the mod folder exactly how they would be in a normal instance of minecraft
C. It *ideally* just works. Still testing.





This repository is meant to enhance the mcheli minecraft mod far beyond what it is capable of. Any and all help would be resourceful. This is a derivative of the source code found [[here](https://github.com/RagexPrince683/MCH-defaultmaster)] 





FAQ:
1. Why is it called mocmaster if it's for mcheli overdrive?
This repository was originally called mocmaster after Moc the guy who showed me the original mcheli backend code, since then I've broken a few repos and have resorted to this being my main Mcheli-Overdrive repository.
2. How to use?
   
I use and set this up with intellij

you need jdk-8u361-windows and if your on 64x 64x 32 for 32.

JDK 8u361
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-linux-i586.rpm
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-linux-i586.tar.gz
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-linux-x64.rpm
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-linux-x64.tar.gz
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-macosx-x64.dmg
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-solaris-sparcv9.tar.gz
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-windows-i586.exe
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-windows-i586.zip
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-windows-x64.exe
https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-windows-x64.zip

open gradle console you should see gradle and then a bunch of things, then you want to type

build

then if all goes well go to build/libs and a java file will be there. If you have made edits to the src you want to open the jar file with a archiver tool such as winrar. You can put this compiled class code into mcheli/mcheli in a actual mcheli instance/folder/the yknow mod and test things out yourself. If you want to make sure there are no errors just copy and paste over the mcheli/mcheli with this stuff.
   
3. Why so many branches?

I am bad at coding/this project has taken my soul


What to do if something breaks?:

1. Close IntelliJ and delete the garbage cache
   Close IntelliJ completely.

In your McheliO project folder, delete:

.gradle/
build/
out/ (if exists)

2. Re-open the project as a Gradle project
   Open IntelliJ.

Do NOT use “New Project” or “Import from existing sources.”

Use File → Open… → select your McheliO root folder (where build.gradle lives).

When prompted, import as a Gradle project.

3. Make sure SDK is Java 8
   File → Project Structure → Project SDK → Set to Java 1.8.

Also set Project language level to 8 - Lambdas, type annotations, etc.

4. Force Gradle to rebuild MCP
   Open the terminal in IntelliJ (or system terminal in the project root) and run:

gradlew clean
gradlew setupDecompWorkspace --refresh-dependencies
gradlew genIntellijRuns

5. Re-sync Gradle
   In IntelliJ, click the little elephant icon (Gradle tool window) → “Reload All Gradle Projects.”

Wait for indexing to finish (top-right progress bar).

6. Check sources
   Right-click src/main/java → Mark Directory As → Sources Root

Right-click src/main/resources → Mark Directory As → Resources Root
