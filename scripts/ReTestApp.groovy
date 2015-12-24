/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import grails.test.runtime.GrailsApplicationTestPlugin
import grails.test.runtime.TestRuntime
import grails.test.runtime.TestRuntimeFactory
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import grails.util.Holders
import org.grails.datastore.mapping.simple.SimpleMapDatastore

/**
 * Gant script that runs the Grails unit tests
 *
 * @author Graeme Rocher
 * @author Luke Daley
 * @since 0.4
 */
includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsTest")

TEST_PHASE_AND_TYPE_SEPARATOR = projectTestRunner.TEST_PHASE_AND_TYPE_SEPARATOR

/**
 * Initialises a persistence context and bootstraps the application.
 */
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

integrationTestPhaseCleanUp = {
    integrationPhaseConfigurer.doCleanup()
}

target('default': "Run a Grails applications unit tests") {
    depends(checkVersion, configureProxy, cleanTestReports)
    try {
        TestRuntimeFactory.removePluginClass(GrailsApplicationTestPlugin)
        TestRuntimeFactory.addPluginClass(HackedGrailsApplicationTestPlugin)
        if (!Holders.grailsApplication?.mainContext) {
            allTests()
            while (!System.console().readLine("Run the tests one more time?").startsWith("q")) {
                allTests()
            }

            integrationTestPhaseCleanUp()
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
        if (!Holders.applicationContext.containsBean("simpleMapDatastore")) {
            Holders.applicationContext.beanFactory.registerSingleton("simpleMapDatastore", new SimpleMapDatastore())
        }
    }

    @Override
    void initialState() {}

    @Override
    void defineBeans(TestRuntime runtime, List<Closure> callables, RuntimeSpringConfiguration targetSpringConfig = null, boolean parent = false) {}
}
