*grails-revolver* is a Grails plugin for a little more efficient development. The plugin name is inspired by [sbt-revolver](https://github.com/spray/sbt-revolver).

Basically, it tries to minimize impact from Grails minimum-20-sec-startup-time by invoking any grails script reusing existing application context.

#Installation
Add plugin in your BuildConfig.groovy:
```groovy
test ":grails-revolver:0.3.0"
```

#Usage
Well, it is able to run any Grails script, but the script itself should not load Grails application, if it is loaded already:
```groovy
grails.plugins.revolver.GrailsRevolver.run("test-app integration:")
```
Unfortunatelly, the 'test-app' command will not work this way, so for that there is a 're-test-app' script, which supports it.
From within "grails shell" invoke the following:
```groovy
grails.plugins.revolver.GrailsRevolver.run("re-test-app integration: org.my.company.MyFavouriteSpec")
// same as
grails.plugins.revolver.GrailsRevolver.testApp("integration: org.my.company.MyFavouriteSpec")
```
Which will not load a new Grails application from scratch, but reuse same loaded application.

Also you can directly use the 're-test-app':
```bash
grails test re-test-app integration: org.my.company.MyFavouriteSpec
```
Which will rerun tests every time Grails detects changes in any source files.
