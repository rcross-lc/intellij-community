// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.workspace.jps.entities

import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.workspace.storage.*
import com.intellij.platform.workspace.storage.annotations.Child
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NonNls

/**
 * Describes a [ContentEntry][com.intellij.openapi.roots.ContentEntry].
 * See [package documentation](psi_element://com.intellij.platform.workspace.jps.entities) for more details.
 */
interface ContentRootEntity : WorkspaceEntity {
    val module: ModuleEntity

    @EqualsBy
    val url: VirtualFileUrl
    val excludedPatterns: List<@NlsSafe String>

    val sourceRoots: List<@Child SourceRootEntity>
    val excludedUrls: List<@Child ExcludeUrlEntity>

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<ContentRootEntity> {
    override var entitySource: EntitySource
    var module: ModuleEntity.Builder
    var url: VirtualFileUrl
    var excludedPatterns: MutableList<String>
    var sourceRoots: List<SourceRootEntity.Builder>
    var excludedUrls: List<ExcludeUrlEntity.Builder>
  }

  companion object : EntityType<ContentRootEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      url: VirtualFileUrl,
      excludedPatterns: List<String>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.url = url
      builder.excludedPatterns = excludedPatterns.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion

}

//region generated code
fun MutableEntityStorage.modifyEntity(
  entity: ContentRootEntity,
  modification: ContentRootEntity.Builder.() -> Unit,
): ContentRootEntity {
  return modifyEntity(ContentRootEntity.Builder::class.java, entity, modification)
}

@get:ApiStatus.Internal
@set:ApiStatus.Internal
var ContentRootEntity.Builder.excludeUrlOrder: @Child ExcludeUrlOrderEntity.Builder?
  by WorkspaceEntity.extensionBuilder(ExcludeUrlOrderEntity::class.java)
@get:ApiStatus.Internal
@set:ApiStatus.Internal
var ContentRootEntity.Builder.sourceRootOrder: @Child SourceRootOrderEntity.Builder?
  by WorkspaceEntity.extensionBuilder(SourceRootOrderEntity::class.java)
//endregion

val ExcludeUrlEntity.contentRoot: ContentRootEntity? by WorkspaceEntity.extension()


data class SourceRootTypeId(val name: @NonNls String)

/**
 * Describes a [SourceFolder][com.intellij.openapi.roots.SourceFolder].
 * See [package documentation](psi_element://com.intellij.platform.workspace.jps.entities) for more details.
 */
interface SourceRootEntity : WorkspaceEntity {
    val contentRoot: ContentRootEntity

    val url: VirtualFileUrl
    val rootTypeId: SourceRootTypeId

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<SourceRootEntity> {
    override var entitySource: EntitySource
    var contentRoot: ContentRootEntity.Builder
    var url: VirtualFileUrl
    var rootTypeId: SourceRootTypeId
  }

  companion object : EntityType<SourceRootEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      url: VirtualFileUrl,
      rootTypeId: SourceRootTypeId,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.url = url
      builder.rootTypeId = rootTypeId
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion

}

//region generated code
fun MutableEntityStorage.modifyEntity(
  entity: SourceRootEntity,
  modification: SourceRootEntity.Builder.() -> Unit,
): SourceRootEntity {
  return modifyEntity(SourceRootEntity.Builder::class.java, entity, modification)
}

@get:ApiStatus.Internal
@set:ApiStatus.Internal
var SourceRootEntity.Builder.customSourceRootProperties: @Child CustomSourceRootPropertiesEntity.Builder?
  by WorkspaceEntity.extensionBuilder(CustomSourceRootPropertiesEntity::class.java)
//endregion
