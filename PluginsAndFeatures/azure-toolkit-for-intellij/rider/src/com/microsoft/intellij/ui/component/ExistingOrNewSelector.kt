/**
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.component

import com.intellij.util.ui.JBUI
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionListener
import javax.swing.JPanel
import javax.swing.JRadioButton

class ExistingOrNewSelector(name: String) : JPanel(MigLayout("novisualpadding, ins 0")), AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(7)
    }

    val rdoUseExisting = JRadioButton("Use Existing $name", true)
    val rdoCreateNew = JRadioButton("Create New $name")

    val isCreateNew: Boolean
        get() = rdoCreateNew.isSelected

    init {
        initAppSelectorButtonGroup()

        add(rdoUseExisting)
        add(rdoCreateNew, "gapbefore $indentionSize")
    }

    private fun initAppSelectorButtonGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExisting to ActionListener { },
                rdoCreateNew to ActionListener { }))
    }
}
