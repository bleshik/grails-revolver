import grails.test.runtime.GrailsApplicationTestPlugin
import grails.test.runtime.TestRuntime
import org.codehaus.groovy.grails.compiler.DirectoryWatcher
import org.codehaus.groovy.grails.project.compiler.GrailsProjectWatcher

import java.util.concurrent.atomic.AtomicBoolean
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import grails.test.runtime.TestRuntimeFactory
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import grails.util.Holders
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
            return;
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

            IntegrationTestPhaseConfigurer.currentApplicationContext = Holders.grailsApplication.mainContext as GrailsWebApplicationContext

            allTests()

            // listen for changes and rerun tests, if any changes are detected
            def watcher = new GrailsProjectWatcher(projectLoader.projectPackager.projectCompiler, Holders.grailsApplication.mainContext.getBean(GrailsPluginManager))
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
                if (changed.compareAndSet(true, false)) {
                    allTests()
                } else {
                    Thread.sleep(1000)
                }
            }

        } else {
            IntegrationTestPhaseConfigurer.currentApplicationContext = Holders.grailsApplication.mainContext as GrailsWebApplicationContext
            binding.setVariable('appCtx', Holders.grailsApplication.mainContext)
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
        runtime.putValue("grailsApplication", Holders.grailsApplication)
        Holders.grailsApplication.classLoader.loadClass('grails.plugins.revolver.GrailsRevolver').fixHolders()
        if (!Holders.applicationContext.containsBean("simpleMapDatastore")) {
            Holders.applicationContext.beanFactory.registerSingleton("simpleMapDatastore", new SimpleMapDatastore())
        }
    }

    @Override
    void initialState() {}

    @Override
    void defineBeans(TestRuntime runtime, List<Closure> callables, RuntimeSpringConfiguration targetSpringConfig = null, boolean parent = false) {}
}
