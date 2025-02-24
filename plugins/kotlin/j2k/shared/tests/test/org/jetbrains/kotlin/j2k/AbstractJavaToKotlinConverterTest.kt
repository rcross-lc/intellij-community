// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.base.test.IgnoreTests.DIRECTIVES.IGNORE_K1
import org.jetbrains.kotlin.idea.base.test.IgnoreTests.DIRECTIVES.IGNORE_K2
import org.jetbrains.kotlin.idea.base.test.KotlinRoot
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

private val ignoreDirectives: Set<String> = setOf(IGNORE_K1, IGNORE_K2)

abstract class AbstractJavaToKotlinConverterTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = J2K_PROJECT_DESCRIPTOR

    protected fun addFile(fileName: String, dirName: String? = null) {
        addFile(File(KotlinRoot.DIR, "j2k/shared/tests/testData/$fileName"), dirName)
    }

    protected fun addFile(file: File, dirName: String? = null): VirtualFile {
        return addFile(FileUtil.loadFile(file, true), file.name, dirName)
    }

    protected fun addFile(text: String, fileName: String, dirName: String?): VirtualFile {
        val filePath = (if (dirName != null) "$dirName/" else "") + fileName
        return myFixture.addFileToProject(filePath, text).virtualFile
    }

    protected fun deleteFile(virtualFile: VirtualFile) {
        runWriteAction { virtualFile.delete(this) }
    }

    protected fun getDisableTestDirective(): String =
        if (isFirPlugin) IGNORE_K2 else IGNORE_K1

    protected fun File.getFileTextWithoutDirectives(): String =
        readText().getTextWithoutDirectives()

    protected fun String.getTextWithoutDirectives(): String =
        split("\n").filterNot { it.trim() in ignoreDirectives }.joinToString(separator = "\n")

    protected fun KtFile.getFileTextWithErrors(): String =
        if (isFirPlugin) getK2FileTextWithErrors(this) else dumpTextWithErrors()

    protected fun addJpaColumnAnnotations() {
        myFixture.addClass(
            """
            package javax.persistence;
            
            import java.lang.annotation.Target;
            import static java.lang.annotation.ElementType.FIELD;
            import static java.lang.annotation.ElementType.METHOD;
            
            @Target({METHOD, FIELD})
            public @interface Column {}
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package jakarta.persistence;
            
            import java.lang.annotation.Target;
            import static java.lang.annotation.ElementType.FIELD;
            import static java.lang.annotation.ElementType.METHOD;
            
            @Target({METHOD, FIELD})
            public @interface Column {}
            """.trimIndent()
        )
    }
}