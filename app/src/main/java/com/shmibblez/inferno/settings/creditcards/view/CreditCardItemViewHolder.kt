/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.creditcards.view

import android.view.View
import mozilla.components.concept.storage.CreditCard
import mozilla.components.support.utils.creditCardIssuerNetwork
import com.shmibblez.inferno.R
import com.shmibblez.inferno.databinding.CreditCardListItemBinding
import com.shmibblez.inferno.settings.creditcards.interactor.CreditCardsManagementInteractor
import com.shmibblez.inferno.utils.view.ViewHolder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * View holder for a credit card list item.
 */
class CreditCardItemViewHolder(
    private val view: View,
    private val interactor: CreditCardsManagementInteractor,
) : ViewHolder(view) {

    fun bind(creditCard: CreditCard) {
        val binding = CreditCardListItemBinding.bind(view)

        binding.creditCardLogo.setImageResource(creditCard.cardType.creditCardIssuerNetwork().icon)

        binding.creditCardNumber.text = creditCard.obfuscatedCardNumber

        bindCreditCardExpiryDate(creditCard, binding)

        itemView.setOnClickListener {
            interactor.onSelectCreditCard(creditCard)
        }
    }

    /**
     * Set the credit card expiry date formatted according to the locale.
     */
    private fun bindCreditCardExpiryDate(
        creditCard: CreditCard,
        binding: CreditCardListItemBinding,
    ) {
        val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        // Subtract 1 from the expiry month since Calendar.Month is based on a 0-indexed.
        calendar.set(Calendar.MONTH, creditCard.expiryMonth.toInt() - 1)
        calendar.set(Calendar.YEAR, creditCard.expiryYear.toInt())

        binding.expiryDate.text = dateFormat.format(calendar.time)
    }

    companion object {
        val LAYOUT_ID
 = R.layout.credit_card_list_item

        // Date format pattern for the credit card expiry date.
        private const val DATE_PATTERN = "MM/yyyy"
    }
}
