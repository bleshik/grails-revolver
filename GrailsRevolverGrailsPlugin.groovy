class GrailsRevolverGrailsPlugin {
    def version = "0.1.0"
    def grailsVersion = "2.4 > *"
    def pluginExcludes = [ "grails-app/views/error.gsp" ]
    def title = "Grails Revolver Plugin" // Headline display name of the plugin
    def author = "Alexey Balchunas"
    def authorEmail = "bleshik@gmail.com"
    def description = '''Plugin for a little more efficient development using Grails'''
    def documentation = "https://github.com/bleshik/grails-revolver"
    def license = "APACHE"
    def developers = [ [ name: "Alexey Balchunas", email: "bleshik@gmail.com" ]]
    def issueManagement = [ system: "GitHub", url: "https://github.com/bleshik/grails-revolver/issues" ]
    def scm = [ url: "https://github.com/bleshik/grails-revolver" ]
    def doWithWebDescriptor = { xml -> }
    def doWithSpring = { }
    def doWithDynamicMethods = { ctx -> }
    def doWithApplicationContext = { ctx -> }
    def onChange = { event -> }
    def onConfigChange = { event -> }
    def onShutdown = { event -> }
}
