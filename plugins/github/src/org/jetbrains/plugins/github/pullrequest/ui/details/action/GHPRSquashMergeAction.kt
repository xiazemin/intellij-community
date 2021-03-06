// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.pullrequest.ui.details.action

import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.pullrequest.ui.details.GHPRStateModel
import java.awt.event.ActionEvent

internal class GHPRSquashMergeAction(stateModel: GHPRStateModel)
  : GHPRMergeAction(GithubBundle.message("pull.request.merge.squash.action"), stateModel) {

  override fun actionPerformed(e: ActionEvent?) = stateModel.submitSquashMergeTask()
}