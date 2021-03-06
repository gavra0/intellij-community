// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.core.script

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.codeInsight.daemon.impl.TrafficLightRenderer
import com.intellij.codeInsight.daemon.impl.TrafficLightRendererContributor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.psi.KtFile

class ScriptTrafficLightRendererContributor : TrafficLightRendererContributor {
    override fun createRenderer(editor: Editor, file: PsiFile?): TrafficLightRenderer? =
        if ((file as? KtFile)?.isScript() == true) {
            ScriptTrafficLightRenderer(file.project, editor.document, file)
        } else null

    class ScriptTrafficLightRenderer(project: Project, document: Document, private val file: KtFile) :
        TrafficLightRenderer(project, document) {
        override fun getDaemonCodeAnalyzerStatus(severityRegistrar: SeverityRegistrar): DaemonCodeAnalyzerStatus {
            val status = super.getDaemonCodeAnalyzerStatus(severityRegistrar)

            val configurations = ScriptConfigurationManager.getServiceIfCreated(project)
            if (configurations == null) {
                // services not yet initialized (it should be initialized under the LoadScriptDefinitionsStartupActivity)
                status.reasonWhySuspended = KotlinIdeaCoreBundle.message("text.loading.kotlin.script.configuration")
                status.errorAnalyzingFinished = false
            } else if (!ScriptDefinitionsManager.getInstance(project).isReady()) {
                status.reasonWhySuspended = KotlinIdeaCoreBundle.message("text.loading.kotlin.script.definitions")
                status.errorAnalyzingFinished = false
            } else if (configurations.isConfigurationLoadingInProgress(file)) {
                status.reasonWhySuspended = KotlinIdeaCoreBundle.message("text.loading.kotlin.script.configuration")
                status.errorAnalyzingFinished = false
            }
            return status
        }
    }
}
