/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.appservice

import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.helpers.validator.WebAppValidator
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * Component can be used for Applications in App Services:
 *   - Web App
 *   - Function App
 */
class AppNameComponent(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 3", "[min!][][min!]")),
        AzureComponent {

    private val lblAppName = JLabel("App Name")
    val txtAppName = JTextField()
    private val lblAppNameSuffix = JLabel(".azurewebsites.net")

    init {
        add(lblAppName)
        add(txtAppName, "growx")
        add(lblAppNameSuffix)

        initComponentValidation()
    }

    override fun initComponentValidation() {
        txtAppName.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = {
                    WebAppValidator.checkInvalidCharacters(txtAppName.text)
                            .merge(WebAppValidator.checkNameMaxLength(txtAppName.text)) },
                focusLostValidationAction = {
                    WebAppValidator.checkNameMinLength(txtAppName.text)
                            .merge(WebAppValidator.checkStartsEndsWithDash(txtAppName.text)) })
    }
}