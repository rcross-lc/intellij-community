// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.documentation.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement

@JvmField
internal val LOG: Logger = Logger.getInstance("#com.intellij.lang.documentation.psi")

fun psiLookupElementDocumentationTargets(element: PsiElement, originalElement: PsiElement?): List<DocumentationTarget> =
  psiDocumentationTargetsImpl(
    element,
    originalElement,
    PsiDocumentationTargetProvider.LOOKUP_ELEMENT_EP_NAME.extensionList + PsiDocumentationTargetProvider.EP_NAME.extensionList
  )

fun psiDocumentationTargets(element: PsiElement, originalElement: PsiElement?): List<DocumentationTarget> =
  psiDocumentationTargetsImpl(
    element,
    originalElement,
    PsiDocumentationTargetProvider.EP_NAME.extensionList
  )

private fun psiDocumentationTargetsImpl(element: PsiElement, originalElement: PsiElement?, providers: List<PsiDocumentationTargetProvider>): List<DocumentationTarget> {
  for (ext in providers) {
    val targets = ext.documentationTargets(element, originalElement)
    if (targets.isNotEmpty()) return targets
  }
  return listOf(PsiElementDocumentationTarget (element.project, element, originalElement))
  //val targets = PsiDocumentationTargetProvider.EP_NAME.extensionList.flatMap { it.documentationTargets(element, originalElement) }
  //return targets.ifEmpty { listOf(PsiElementDocumentationTarget (element.project, element, originalElement)) }
}

fun createPsiDocumentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget =
  PsiElementDocumentationTarget(element.project, element, originalElement)

internal fun isNavigatableQuickDoc(source: PsiElement?, target: PsiElement): Boolean {
  return target !== source && target !== source?.parent
}
