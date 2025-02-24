// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@file:JvmName("NavBarIdeBundle")
@file:Internal

package com.intellij.platform.navbar.ide

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.lang.invoke.MethodHandles
import java.util.function.Supplier

private const val BUNDLE_FQN: @NonNls String = "messages.NavBarIdeBundle"
private val BUNDLE = DynamicBundle(MethodHandles.lookup().lookupClass(), BUNDLE_FQN)

fun message(
  key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
  vararg params: Any
): @Nls String {
  return BUNDLE.getMessage(key, *params)
}

fun messagePointer(
  key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
  vararg params: Any
): Supplier<String> {
  return BUNDLE.getLazyMessage(key, *params)
}
