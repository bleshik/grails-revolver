package grails.plugins.revolver

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.Holders
import groovy.transform.CompileStatic

import java.lang.reflect.Field

import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.GrailsApplicationDiscoveryStrategy
import org.springframework.context.ApplicationContext

@CompileStatic
final class GrailsRevolver {
    private GrailsRevolver() {}


    /**
     * Runs command without loading the actual project from scratch. So basically it will simply execute the Gant script.
     */
    static void run(String cmd) {
        BuildSettings settings = BuildSettingsHolder.settings
        fixHolders()
        try {
            GrailsScriptRunner runner = new GrailsScriptRunner(settings)

            // Since the runner is not supposed to be used like that,
            // we have to initialize some private things by ourselves
            Field scriptCacheDirField = GrailsScriptRunner.getDeclaredField('scriptCacheDir')
            scriptCacheDirField.setAccessible(true)
            Field classLoaderField = GrailsScriptRunner.getDeclaredField('classLoader')
            classLoaderField.setAccessible(true)
            scriptCacheDirField.set(runner, new File(settings.getProjectWorkDir(), "scriptCache"))
            classLoaderField.set(runner, Holders.grailsApplication.classLoader)

            runner.executeScriptWithCaching(GrailsScriptRunner.getCommandLineParser().parseString(cmd))
        } finally {
            // GrailsScriptRunner sets null here after execution, so we put it back
            BuildSettingsHolder.settings = settings
        }
    }

    /**
     * Runs tests using existing application context.
     * Note, that in order to do that, we provide our script for running tests: ReTestApp.
     * The grails core TestApp won't work, because it clears and shuts down everything, so the second execution doesn't work.
     */
    static void testApp(String cmd = "") {
        run("re-test-app " + cmd)
    }

    /**
     * Makes it possible to run commands from within "grails shell".
     * If the class is used from within "grails shell", {@link Holders} does not contains discovery strategies,
     * this returns no application context.
     * To fix that, we simply add dummy discovery strategy using {@link Holders#getGrailsApplication()}.
     */
     static void fixHolders() {
        if (!Holders.findApplicationContext() && Holders.grailsApplication) {
            Holders.addApplicationDiscoveryStrategy(new GrailsApplicationDiscoveryStrategy() {
                @Override
                GrailsApplication findGrailsApplication() { null }

                @Override
                ApplicationContext findApplicationContext() { Holders.grailsApplication.mainContext }
            })
        }
    }
}
