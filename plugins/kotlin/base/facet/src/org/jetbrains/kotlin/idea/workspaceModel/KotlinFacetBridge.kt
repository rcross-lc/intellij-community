// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.workspaceModel

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ExternalProjectSystemRegistry
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.impl.legacyBridge.facet.FacetBridge
import com.intellij.workspaceModel.ide.impl.legacyBridge.facet.FacetConfigurationBridge
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration

class KotlinFacetBridge(
    module: Module,
    name: String,
    configuration: KotlinFacetConfiguration
) : KotlinFacet(module, name, configuration),
    FacetBridge<KotlinSettingsEntity, KotlinSettingsEntity.Builder> {
    override val config: FacetConfigurationBridge<KotlinSettingsEntity, KotlinSettingsEntity.Builder>
        get() = configuration as KotlinFacetConfigurationBridge

    override fun updateExistingEntityInStorage(existingFacetEntity: KotlinSettingsEntity, mutableStorage: MutableEntityStorage) {
        val moduleEntity = mutableStorage.resolve(existingFacetEntity.moduleId)!!
        mutableStorage.modifyEntity(moduleEntity) module1@{
            val kotlinSettingsEntity = config.getEntityBuilder(this@module1)
            val resolvedModule: ModuleEntity = mutableStorage.resolve(kotlinSettingsEntity.moduleId)!!
            mutableStorage.modifyEntity(resolvedModule) module2@{
                mutableStorage.modifyEntity(existingFacetEntity) {
                    if (kotlinSettingsEntity.flushNeeded) flushNeeded = false
                    name = kotlinSettingsEntity.name
                    sourceRoots = kotlinSettingsEntity.sourceRoots.toMutableList()
                    configFileItems = kotlinSettingsEntity.configFileItems.toMutableList()
                    moduleId = kotlinSettingsEntity.moduleId
                    module = this@module2
                    useProjectSettings = kotlinSettingsEntity.useProjectSettings
                    implementedModuleNames = kotlinSettingsEntity.implementedModuleNames.toMutableList()
                    dependsOnModuleNames = kotlinSettingsEntity.dependsOnModuleNames.toMutableList()
                    additionalVisibleModuleNames = kotlinSettingsEntity.additionalVisibleModuleNames.toMutableSet()
                    productionOutputPath = kotlinSettingsEntity.productionOutputPath
                    testOutputPath = kotlinSettingsEntity.testOutputPath
                    sourceSetNames = kotlinSettingsEntity.sourceSetNames.toMutableList()
                    isTestModule = kotlinSettingsEntity.isTestModule
                    externalProjectId = kotlinSettingsEntity.externalProjectId
                    isHmppEnabled = kotlinSettingsEntity.isHmppEnabled
                    pureKotlinSourceFolders = kotlinSettingsEntity.pureKotlinSourceFolders.toMutableList()
                    kind = kotlinSettingsEntity.kind
                    compilerArguments = kotlinSettingsEntity.compilerArguments
                    compilerSettings = kotlinSettingsEntity.compilerSettings
                    targetPlatform = kotlinSettingsEntity.targetPlatform
                    externalSystemRunTasks = kotlinSettingsEntity.externalSystemRunTasks.toMutableList()
                    version = kotlinSettingsEntity.version
                }
            }
        }
    }

    override fun getExternalSource(): ProjectModelExternalSource? {
        return super.getExternalSource() ?: run {
            val modulePropertyManager = ExternalSystemModulePropertyManager.getInstance(module)
            modulePropertyManager.getExternalSystemId()?.let { externalSystemId ->
                ExternalProjectSystemRegistry.getInstance().getSourceById(externalSystemId)
            }
        }
    }
}