import grails.test.runtime.GrailsApplicationTestPlugin
import grails.test.runtime.TestRuntime
import java.io.BufferedReader
import java.io.InputStreamReader
import grails.test.runtime.TestRuntimeFactory
import grails.util.Holders
import java.util.concurrent.atomic.AtomicBoolean
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.compiler.DirectoryWatcher
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.project.compiler.GrailsProjectWatcher
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import org.grails.datastore.mapping.simple.SimpleMapDatastore

grailsSettings.defaultEnv = true
scriptEnv = 'test'

includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsTest")
includeTargets << grailsScript('_GrailsBootstrap')

TEST_PHASE_AND_TYPE_SEPARATOR = projectTestRunner.TEST_PHASE_AND_TYPE_SEPARATOR

integrationPhaseConfigurer = new IntegrationTestPhaseConfigurer(projectTestRunner.projectTestCompiler, projectLoader) {

    @Override
    void prepare(Binding testExecutionContext, Map<String, Object> testOptions) {
        if (currentApplicationContext) {
            return
        }
        super.prepare(testExecutionContext, testOptions)
    }

    @Override
    void cleanup(Binding testExecutionContext, Map<String, Object> testOptions) {
        /* do nothing */
    }

    void doCleanup() {
        super.cleanup()
    }

}

projectTestRunner.testFeatureDiscovery.configurers.integration = integrationPhaseConfigurer

target('default': "Run a Grails applications unit tests") {
    depends(checkVersion, configureProxy, cleanTestReports)
    try {
        TestRuntimeFactory.removePluginClass(GrailsApplicationTestPlugin)
        TestRuntimeFactory.addPluginClass(HackedGrailsApplicationTestPlugin)
        if (!Holders.grailsApplication?.mainContext) {
            depends(compile, bootstrap)

            Holders.grailsApplication.classLoader.loadClass('grails.plugins.revolver.GrailsRevolver').fixHolders()
            GrailsWebApplicationContext applicationContext = Holders.grailsApplication.mainContext
            IntegrationTestPhaseConfigurer.currentApplicationContext = applicationContext

            allTests()

            println "Change source files or press any key to rerun the tests"

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in))
            // listen for changes and rerun tests, if any changes are detected
            def watcher = new GrailsProjectWatcher(projectLoader.projectPackager.projectCompiler, applicationContext.getBean(GrailsPluginManager))
            watcher.start()
            AtomicBoolean changed = new AtomicBoolean()
            watcher.addListener(new DirectoryWatcher.FileChangeListener() {
                void onChange(File file) {
                    changed.set(true)
                }

                void onNew(File file) {
                    changed.set(true)
                }
            })
            while (true) {
                if (changed.compareAndSet(true, false) || input.ready()) {
                    allTests()
                    // reset the flag and the input, so that we ignore the 'rerun triggers' happened during the last test run
                    changed.set(false)
                    while(input.ready()) { input.read() }
                } else {
                    sleep(1000)
                }
            }

        } else {
            GrailsWebApplicationContext applicationContext = Holders.grailsApplication.mainContext
            IntegrationTestPhaseConfigurer.currentApplicationContext = applicationContext
            binding.setVariable('appCtx', applicationContext)
            allTests()
        }
    } finally {
        TestRuntimeFactory.removePluginClass(HackedGrailsApplicationTestPlugin)
        TestRuntimeFactory.addPluginClass(GrailsApplicationTestPlugin)
    }
}

class HackedGrailsApplicationTestPlugin extends GrailsApplicationTestPlugin {
    @Override
    void shutdownApplicationContext(TestRuntime runtime) {
        Holders.applicationContext.beanFactory.destroySingleton("simpleMapDatastore")
    }

    @Override
    void initGrailsApplication(final TestRuntime runtime, final Map callerInfo) {
        def grailsApplication = Holders.grailsApplication
        def applicationContext = Holders.applicationContext
        runtime.putValue("grailsApplication", grailsApplication)
        if (!applicationContext.containsBean("simpleMapDatastore")) {
            applicationContext.beanFactory.registerSingleton("simpleMapDatastore", new SimpleMapDatastore())
        }
    }

    @Override
    void initialState() {}

    @Override
    void defineBeans(TestRuntime runtime, List<Closure> callables, RuntimeSpringConfiguration targetSpringConfig = null, boolean parent = false) {}
}
