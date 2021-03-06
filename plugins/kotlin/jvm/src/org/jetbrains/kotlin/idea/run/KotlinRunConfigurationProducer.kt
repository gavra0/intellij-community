// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinRunConfigurationProducer : LazyRunConfigurationProducer<KotlinRunConfiguration>() {
    override fun setupConfigurationFromContext(
        configuration: KotlinRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val location = context.location ?: return false
        val module = location.module?.asJvmModule() ?: return false
        val container = getEntryPointContainer(location) ?: return false
        val startClassFQName = getStartClassFqName(container) ?: return false

        configuration.setModule(module)
        configuration.runClass = startClassFQName
        configuration.setGeneratedName()

        return true
    }

    private fun getEntryPointContainer(location: Location<*>?): KtDeclarationContainer? {
        if (location == null) return null
        if (DumbService.getInstance(location.project).isDumb) return null

        return getEntryPointContainer(location.psiElement)
    }

    override fun isConfigurationFromContext(configuration: KotlinRunConfiguration, context: ConfigurationContext): Boolean {
        val entryPointContainer = getEntryPointContainer(context.location) ?: return false
        val startClassFQName = getStartClassFqName(entryPointContainer) ?: return false

        return configuration.runClass == startClassFQName &&
                context.module?.asJvmModule() == configuration.configurationModule?.module
    }

    companion object {
        fun getEntryPointContainer(locationElement: PsiElement): KtDeclarationContainer? {
            val psiFile = locationElement.containingFile
            if (!(psiFile is KtFile && ProjectRootsUtil.isInProjectOrLibSource(psiFile))) return null

            val mainFunctionDetector =
                MainFunctionDetector(psiFile.languageVersionSettings) { it.resolveToDescriptorIfAny(BodyResolveMode.FULL) }

            var currentElement = locationElement.declarationContainer(false)
            while (currentElement != null) {
                var entryPointContainer = currentElement
                if (entryPointContainer is KtClass) {
                    entryPointContainer = entryPointContainer.companionObjects.singleOrNull()
                }
                if (entryPointContainer != null && mainFunctionDetector.hasMain(entryPointContainer.declarations)) return entryPointContainer
                currentElement = (currentElement as PsiElement).declarationContainer(true)
            }

            return null
        }

        fun getStartClassFqName(container: KtDeclarationContainer): String? = when (container) {
            is KtFile -> container.javaFileFacadeFqName.asString()
            is KtClassOrObject -> {
                if (!container.isValid) {
                    null
                } else if (container is KtObjectDeclaration && container.isCompanion()) {
                    val containerClass = container.getParentOfType<KtClass>(true)
                    containerClass?.toLightClass()?.let { ClassUtil.getJVMClassName(it) }
                } else {
                    container.toLightClass()?.let { ClassUtil.getJVMClassName(it) }
                }
            }
            else -> null
        }

        private fun PsiElement.declarationContainer(strict: Boolean): KtDeclarationContainer? {
            val element = if (strict)
                PsiTreeUtil.getParentOfType(this, KtClassOrObject::class.java, KtFile::class.java)
            else
                PsiTreeUtil.getNonStrictParentOfType(this, KtClassOrObject::class.java, KtFile::class.java)
            return element
        }
    }

    override fun getConfigurationFactory(): ConfigurationFactory = KotlinRunConfigurationType.instance
}
