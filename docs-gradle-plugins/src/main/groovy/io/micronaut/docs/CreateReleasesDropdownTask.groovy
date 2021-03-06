package io.micronaut.docs

import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CreateReleasesDropdownTask extends DefaultTask {

    @Input
    String slug

    @Input
    String version

    @OutputFile
    File doc

    @TaskAction
    void modifyHtmlAndAddReleasesDropdown() {

        String selectHtml = composeSelectHtml()

        String versionHtml = "<p><strong>Version:</strong> ${version}</p>"
        String versionWithSelectHtml = "<p><strong>Version:</strong> ${selectHtml.replaceAll("style='margin-top: 10px'", "style='max-width: 200px'")}</p>"
        doc.text = doc.text.replace(versionHtml, versionWithSelectHtml)
    }

    String composeSelectHtml() {
        String repo = slug.split('/')[1]
        String org = slug.split('/')[0]
        JsonSlurper slurper = new JsonSlurper()
        String json = new URL("https://api.github.com/repos/${slug}/tags").text
        def result = slurper.parseText(json)

        String selectHtml = "<select style='margin-top: 10px' onChange='window.document.location.href=this.options[this.selectedIndex].value;'>"
        String snapshotHref = "https://${org}.github.io/${repo}/snapshot/guide/index.html"
        if (version.endsWith('BUILD-SNAPSHOT')) {
            selectHtml += "<option selected='selected' value='${snapshotHref}'>SNAPSHOT</option>"
        } else {
            selectHtml += "<option value='${snapshotHref}'>SNAPSHOT</option>"
        }
        parseSoftwareVersions(result).each { softwareVersion ->
            String versionName = softwareVersion.versionText
            String href = "https://${org}.github.io/${repo}/${versionName}/guide/index.html"
            if (slug == 'micronaut-projects/micronaut-core') {
                href = "https://docs.micronaut.io/${versionName}/guide/index.html"
            }
            if (version == versionName) {
                selectHtml += "<option selected='selected' value='${href}'>${versionName}</option>"
            } else {
                selectHtml += "<option value='${href}'>${versionName}</option>"
            }
        }
        selectHtml += '</select>'
        selectHtml
    }

    @CompileDynamic
    List<SoftwareVersion> parseSoftwareVersions(Object result) {
        result.findAll { it.name.startsWith('v') }.collect { SoftwareVersion.build(it.name.replace('v', '')) }.sort().reverse()
    }
}
