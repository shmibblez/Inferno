/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.creditcards.interactor

import mozilla.components.concept.storage.CreditCard
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.CreditCards
import com.shmibblez.inferno.settings.creditcards.controller.CreditCardsManagementController

/**
 * Interface for the credit cards management Interactor.
 */
interface CreditCardsManagementInteractor {

    /**
     * Navigates to the credit card editor to edit the selected credit card. Called when a user
     * taps on a credit card item.
     *
     * @param creditCard The selected [CreditCard] to edit.
     */
    fun onSelectCreditCard(creditCard: CreditCard)

    /**
     * Navigates to the credit card editor to add a new credit card. Called when a user
     * taps on 'Add card' button.
     */
    fun onAddCreditCardClick()
}

/**
 * The default implementation of [CreditCardsManagementInteractor].
 *
 * @param controller An instance of [CreditCardsManagementController] which will be delegated for
 * all user interactions.
 */
class DefaultCreditCardsManagementInteractor(
    private val controller: CreditCardsManagementController,
) : CreditCardsManagementInteractor {

    override fun onSelectCreditCard(creditCard: CreditCard) {
        controller.handleCreditCardClicked(creditCard)
//        CreditCards.managementCardTapped.record(NoExtras())
    }

    override fun onAddCreditCardClick() {
        controller.handleAddCreditCardClicked()
//        CreditCards.managementAddTapped.record(NoExtras())
    }
}
