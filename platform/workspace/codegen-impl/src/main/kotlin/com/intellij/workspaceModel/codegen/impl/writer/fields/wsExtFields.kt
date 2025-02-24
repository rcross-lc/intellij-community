// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.workspaceModel.codegen.impl.writer.fields

import com.intellij.workspaceModel.codegen.deft.meta.ExtProperty
import com.intellij.workspaceModel.codegen.impl.writer.*
import com.intellij.workspaceModel.codegen.impl.writer.extensions.*

val ExtProperty<*, *>.wsCode: String
  get() = lines {
    val isChild = valueType.getRefType().child
    val annotation = if (isChild) "@${Child} " else ""
    val generic = if (receiver.builderWithTypeParameter) "<out ${receiver.javaFullName}>" else ""
    sectionNoBrackets("$generatedCodeVisibilityModifier var ${receiver.javaBuilderName}$generic.$name: $annotation${valueType.javaBuilderTypeWithGeneric}") {
      line("by WorkspaceEntity.extensionBuilder(${valueType.entityType}::class.java)")
    }
  }
