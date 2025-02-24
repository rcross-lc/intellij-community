// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.plugins

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.intellij.openapi.application.ModernApplicationStarter
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextLikeFileType
import com.intellij.util.io.jackson.array
import com.intellij.util.io.jackson.obj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo
import kotlin.system.exitProcess

private class BundledPluginsLister : ModernApplicationStarter() {
  override val commandName: String
    get() = "listBundledPlugins"

  // not premain because FileTypeManager is used to report extensions
  override suspend fun start(args: List<String>) {
    try {
      val out: Writer = if (args.size == 2) {
        val outFile = Path.of(args[1])
        withContext(Dispatchers.IO) {
          Files.createDirectories(outFile.parent)
          Files.newBufferedWriter(outFile)
        }
      }
      else {
        // noinspection UseOfSystemOutOrSystemErr
        OutputStreamWriter(System.out, StandardCharsets.UTF_8)
      }
      JsonFactory().createGenerator(out).use { writer ->
        val plugins = PluginManagerCore.getPluginSet().enabledPlugins
        val layout = HashSet<LayoutItemDescriptor>()
        val pluginIds = ArrayList<String>(plugins.size)
        val homeDir = Path.of(PathManager.getHomePath())
        for (plugin in plugins) {
          layout.add(LayoutItemDescriptor(
            name = plugin.pluginId.idString,
            kind = ProductInfoLayoutItemKind.plugin,
            classPath = plugin.jarFiles?.map { it.relativeTo(homeDir).invariantSeparatorsPathString } ?: emptyList()
          ))

          pluginIds.add(plugin.pluginId.idString)
          plugin.pluginAliases.mapTo(layout) {
            LayoutItemDescriptor(name = it.idString, kind = ProductInfoLayoutItemKind.pluginAlias, classPath = emptyList())
          }
          plugin.content.modules.mapTo(layout) {
            LayoutItemDescriptor(
              name = it.name,
              kind = if (plugin.pluginId == PluginManagerCore.CORE_ID) {
                ProductInfoLayoutItemKind.productModuleV2
              }
              else {
                ProductInfoLayoutItemKind.moduleV2
              },
              classPath = it.requireDescriptor().jarFiles?.map {
                file -> file.relativeTo(homeDir).invariantSeparatorsPathString
              } ?: emptyList(),
            )
          }
        }
        pluginIds.sort()
        val fileTypeManager = FileTypeManager.getInstance()
        val extensions = ArrayList<String>()
        for (type in fileTypeManager.registeredFileTypes) {
          if (type !is PlainTextLikeFileType) {
            for (matcher in fileTypeManager.getAssociations(type!!)) {
              extensions.add(matcher.presentableString)
            }
          }
        }
        extensions.sort()

        writer.obj {
          writer.array("layout") {
            for (module in layout.sortedBy { it.name }) {
              writer.obj {
                writer.writeStringField("name", module.name)
                writer.writeStringField("kind", module.kind.name)
                if (module.classPath.isNotEmpty()) {
                  writeList(writer, "classPath", module.classPath)
                }
              }
            }
          }
          writeList(writer, "plugins", pluginIds)
          writeList(writer, "fileExtensions", extensions)
        }
      }
    }
    catch (e: Exception) {
      try {
        logger<BundledPluginsLister>().error("Bundled plugins list builder failed", e)
      }
      catch (ignored: Throwable) { }
      e.printStackTrace(System.err)
      exitProcess(1)
    }

    exitProcess(0)
  }
}

private class LayoutItemDescriptor(
  @JvmField val name: String,
  @JvmField val kind: ProductInfoLayoutItemKind,
  @JvmField val classPath: List<String> = emptyList(),
)

@Suppress("EnumEntryName")
@Serializable
private enum class ProductInfoLayoutItemKind {
  plugin, pluginAlias, productModuleV2, moduleV2
}

private fun writeList(writer: JsonGenerator, name: String, elements: Collection<String>) {
  writer.array(name) {
    for (module in elements) {
      writer.writeString(module)
    }
  }
}