// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.experiment.ab.impl.statistic

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import com.intellij.platform.experiment.ab.impl.experiment.ABExperiment
import com.intellij.platform.experiment.ab.impl.experiment.ABExperiment.Companion.OPTION_ID_FREE_GROUP

class ABExperimentOptionIdValidationRule : CustomValidationRule() {
  override fun getRuleId(): String = "ab_experiment_option_id"

  override fun doValidate(data: String, context: EventContext): ValidationResultType {
    return if (ABExperiment.getJbABExperimentOptionList().any { it.id.value == data } || data == OPTION_ID_FREE_GROUP.value)
      ValidationResultType.ACCEPTED
    else ValidationResultType.REJECTED
  }
}