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

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.*
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.AppServicePlanValidator
import com.microsoft.intellij.helpers.validator.LocationValidator
import com.microsoft.intellij.helpers.validator.PricingTierValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionListener
import javax.swing.*

class AppServicePlanSelector(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]", "[sg a]")),
        AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(17)

        private const val EMPTY_APP_SERVICE_PLAN_MESSAGE = "No existing Azure App Service Plans"
        private const val EMPTY_LOCATION_MESSAGE = "No existing Azure Locations"
        private const val EMPTY_PRICING_TIER_MESSAGE = "No existing Azure Pricing Tiers"

        private const val WEB_APP_PRICING_URI = "https://azure.microsoft.com/en-us/pricing/details/app-service/"
    }

    val rdoUseExisting = JRadioButton("Use Existing", true)
    val cbAppServicePlan = ComboBox<AppServicePlan>()
    private val lblExistingLocationName = JLabel("Location")
    private val lblLocationValue = JLabel("N/A")
    private val lblExistingPricingTierName = JLabel("Pricing Tier")
    private val lblExistingPricingTierValue = JLabel("N/A")

    val rdoCreateNew = JRadioButton("Create New")
    val txtName = JTextField("")
    private val lblCreateLocationName = JLabel("Location")
    val cbLocation = ComboBox<Location>()
    private val pnlCreatePricingTier = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    private val lblCreatePricingTierName = JLabel("Pricing Tier")
    val cbPricingTier = ComboBox<PricingTier>()
    private val lblPricingLink = LinkLabel("Pricing", null, { _, link -> BrowserUtil.browse(link) }, WEB_APP_PRICING_URI)

    var cachedAppServicePlan: List<AppServicePlan> = emptyList()
    var cachedPricingTier: List<PricingTier> = emptyList()

    var lastSelectedAppServicePlan: AppServicePlan? = null

    val isCreatingNew
        get() = rdoCreateNew.isSelected

    init {
        initAppServicePlanComboBox()
        initLocationComboBox()
        initPricingTierComboBox()
        initAppServicePlanButtonsGroup()

        pnlCreatePricingTier.apply {
            add(cbPricingTier, "growx")
            add(lblPricingLink)
        }

        add(rdoUseExisting)
        add(cbAppServicePlan, "growx")
        add(lblExistingLocationName, "gapbefore $indentionSize")
        add(lblLocationValue, "growx")
        add(lblExistingPricingTierName, "gapbefore $indentionSize")
        add(lblExistingPricingTierValue, "growx")

        add(rdoCreateNew)
        add(txtName, "growx")
        add(lblCreateLocationName, "gapbefore $indentionSize")
        add(cbLocation, "growx")
        add(lblCreatePricingTierName, "gapbefore $indentionSize")
        add(pnlCreatePricingTier, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()

        if (rdoUseExisting.isSelected) {
            return listOfNotNull(
                    AppServicePlanValidator.checkAppServicePlanIsSet(cbAppServicePlan.getSelectedValue())
                            .toValidationInfo(cbAppServicePlan)
            )
        }

        return listOfNotNull(
                AppServicePlanValidator.validateAppServicePlanName(txtName.text).toValidationInfo(txtName),
                LocationValidator.checkLocationIsSet(cbLocation.getSelectedValue()).toValidationInfo(cbLocation),
                PricingTierValidator.checkPricingTierIsSet(cbPricingTier.getSelectedValue()).toValidationInfo(cbPricingTier)
        )
    }

    override fun initComponentValidation() {
        txtName.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    AppServicePlanValidator.checkAppServicePlanNameMaxLength(txtName.text)
                            .merge(AppServicePlanValidator.checkInvalidCharacters(txtName.text)) },
                focusLostValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    if (txtName.text.isEmpty()) return@initValidationWithResult ValidationResult()
                    AppServicePlanValidator.checkAppServicePlanNameMinLength(txtName.text) })
    }

    fun fillAppServicePlanComboBox(appServicePlans: List<AppServicePlan>, defaultComparator: (AppServicePlan) -> Boolean = { false }) {
        cachedAppServicePlan = appServicePlans

        cbAppServicePlan.fillComboBox(
                appServicePlans.sortedWith(compareBy({ it.operatingSystem() }, { it.name() })),
                defaultComparator)

        if (appServicePlans.isEmpty()) {
            rdoCreateNew.doClick()
        }
    }

    fun fillLocationComboBox(locations: List<Location>, defaultLocation: Region? = AzureDefaults.location) {
        cbLocation.fillComboBox<Location>(locations) { location -> location.region() == defaultLocation }
    }

    fun fillPricingTier(pricingTiers: List<PricingTier>, defaultPricingTier: PricingTier? = AzureDefaults.pricingTier) {
        cachedPricingTier = pricingTiers
        cbPricingTier.fillComboBox(pricingTiers, defaultPricingTier)
    }

    fun toggleAppServicePlanPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtName, cbLocation, cbPricingTier)
        setComponentsEnabled(!isCreatingNew, cbAppServicePlan, lblLocationValue, lblExistingPricingTierValue)
    }

    private fun initAppServicePlanComboBox() {
        cbAppServicePlan.setDefaultRenderer(EMPTY_APP_SERVICE_PLAN_MESSAGE) { it.name() }

        cbAppServicePlan.addActionListener {
            val plan = cbAppServicePlan.getSelectedValue() ?: return@addActionListener
            if (plan == lastSelectedAppServicePlan) return@addActionListener

            lblLocationValue.text = plan.regionName()
            val pricingTier = plan.pricingTier()
            val skuDescription = pricingTier.toSkuDescription()
            lblExistingPricingTierValue.text = "${skuDescription.name()} (${skuDescription.tier()})"

            lastSelectedAppServicePlan = plan
        }
    }

    private fun initLocationComboBox() {
        cbLocation.setDefaultRenderer(EMPTY_LOCATION_MESSAGE) { it.displayName() }
    }

    private fun initPricingTierComboBox() {
        cbPricingTier.setDefaultRenderer(EMPTY_PRICING_TIER_MESSAGE) {
            val skuDescription = it.toSkuDescription()
            "${skuDescription.name()} (${skuDescription.tier()})"
        }
    }

    private fun initAppServicePlanButtonsGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExisting to ActionListener { toggleAppServicePlanPanel(false) },
                rdoCreateNew to ActionListener { toggleAppServicePlanPanel(true) }))

        toggleAppServicePlanPanel(false)
    }
}