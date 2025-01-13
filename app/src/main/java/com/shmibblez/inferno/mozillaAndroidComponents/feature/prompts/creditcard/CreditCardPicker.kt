/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.creditcard

import androidx.annotation.VisibleForTesting
import com.shmibblez.inferno.mozillaAndroidComponents.browser.state.action.ContentAction
import com.shmibblez.inferno.mozillaAndroidComponents.browser.state.store.BrowserStore
import com.shmibblez.inferno.mozillaAndroidComponents.concept.engine.prompt.PromptRequest
import com.shmibblez.inferno.mozillaAndroidComponents.concept.storage.CreditCardEntry
import com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.concept.AutocompletePrompt
import com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.concept.SelectablePromptView
import com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.consumePromptFrom
import com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.facts.emitCreditCardAutofillDismissedFact
import com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.facts.emitCreditCardAutofillShownFact
import com.shmibblez.inferno.mozillaAndroidComponents.support.base.log.logger.Logger

/**
 * Interactor that implements [SelectablePromptView.Listener] and notifies the feature about actions
 * the user performed in the credit card picker.
 *
 * @property store The [BrowserStore] this feature should subscribe to.
 * @property creditCardSelectBar The [AutocompletePrompt] view into which the select credit card
 * prompt will be inflated.
 * @property manageCreditCardsCallback A callback invoked when a user selects "Manage credit cards"
 * from the select credit card prompt.
 * @property selectCreditCardCallback A callback invoked when a user selects a credit card option
 * from the select credit card prompt
 * @property sessionId The session ID which requested the prompt.
 */
class CreditCardPicker(
    private val store: BrowserStore,
    private val creditCardSelectBar: AutocompletePrompt<CreditCardEntry>,
    private val manageCreditCardsCallback: () -> Unit = {},
    private val selectCreditCardCallback: () -> Unit = {},
    private var sessionId: String? = null,
) : SelectablePromptView.Listener<CreditCardEntry> {

    init {
        creditCardSelectBar.selectablePromptListener = this
    }

    // The selected credit card option to confirm.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var selectedCreditCard: CreditCardEntry? = null

    override fun onManageOptions() {
        manageCreditCardsCallback.invoke()
        dismissSelectCreditCardRequest()
    }

    override fun onOptionSelect(option: CreditCardEntry) {
        selectedCreditCard = option
        creditCardSelectBar.hidePrompt()
        selectCreditCardCallback.invoke()
    }

    /**
     * Called on a successful authentication to confirm the selected credit card option.
     */
    fun onAuthSuccess() {
        store.consumePromptFrom<PromptRequest.SelectCreditCard>(sessionId) {
            selectedCreditCard?.let { creditCard ->
                it.onConfirm(creditCard)
            }

            selectedCreditCard = null
        }
    }

    /**
     * Called on a failed authentication to dismiss the current select credit card prompt request.
     */
    fun onAuthFailure() {
        selectedCreditCard = null

        store.consumePromptFrom<PromptRequest.SelectCreditCard>(sessionId) {
            it.onDismiss()
        }
    }

    /**
     * Dismisses the active select credit card request.
     *
     * @param promptRequest The current active [PromptRequest.SelectCreditCard] or null
     * otherwise.
     */
    @Suppress("TooGenericExceptionCaught")
    fun dismissSelectCreditCardRequest(promptRequest: PromptRequest.SelectCreditCard? = null) {
        emitCreditCardAutofillDismissedFact()
        creditCardSelectBar.hidePrompt()

        try {
            if (promptRequest != null) {
                promptRequest.onDismiss()
                sessionId?.let {
                    store.dispatch(ContentAction.ConsumePromptRequestAction(it, promptRequest))
                }
                return
            }

            store.consumePromptFrom<PromptRequest.SelectCreditCard>(sessionId) {
                it.onDismiss()
            }
        } catch (e: RuntimeException) {
            Logger.error("Can't dismiss this select credit card prompt", e)
        }
    }

    /**
     * Shows the select credit card prompt in response to the [PromptRequest] event.
     *
     * @param request The [PromptRequest] containing the the credit card request data to be shown.
     */
    internal fun handleSelectCreditCardRequest(request: PromptRequest.SelectCreditCard) {
        emitCreditCardAutofillShownFact()
        creditCardSelectBar.showPrompt()
        creditCardSelectBar.populate(request.creditCards)
    }
}
