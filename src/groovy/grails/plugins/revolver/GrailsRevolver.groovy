package grails.plugins.revolver

import grails.test.runtime.GrailsApplicationTestPlugin
import grails.test.runtime.TestRuntime
import org.codehaus.groovy.grails.cli.parsing.DefaultCommandLine
import org.codehaus.groovy.grails.cli.parsing.CommandLineParser
import grails.util.BuildSettingsHolder
import grails.util.GrailsNameUtils
import grails.util.BuildSettings
import grails.util.Holders
import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.GrailsApplicationDiscoveryStrategy
import org.springframework.context.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean

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
            def runner = new GrailsScriptRunner(settings)

            // Since the runner is not supposed to be used like that,
            // we have to initialize some private things by ourselves
            def scriptCacheDirField = GrailsScriptRunner.getDeclaredField('scriptCacheDir').with { setAccessible(true); it }
            def classLoaderField = GrailsScriptRunner.getDeclaredField('classLoader').with { setAccessible(true); it }
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
    private static void fixHolders() {
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
