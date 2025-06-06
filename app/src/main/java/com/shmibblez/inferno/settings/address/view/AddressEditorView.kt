/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.address.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import mozilla.components.browser.state.search.RegionState
import mozilla.components.concept.storage.Address
import mozilla.components.concept.storage.UpdatableAddressFields
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.showKeyboard
import mozilla.components.ui.widgets.withCenterAlignedButtons
//import com.shmibblez.inferno.GleanMetrics.Addresses
import com.shmibblez.inferno.R
import com.shmibblez.inferno.databinding.FragmentAddressEditorBinding
import com.shmibblez.inferno.ext.placeCursorAtEnd
import com.shmibblez.inferno.settings.address.AddressEditorFragment
import com.shmibblez.inferno.settings.address.AddressUtils.countries
import com.shmibblez.inferno.settings.address.Country
import com.shmibblez.inferno.settings.address.DEFAULT_COUNTRY
import com.shmibblez.inferno.settings.address.interactor.AddressEditorInteractor
import com.shmibblez.inferno.settings.address.toCountryCode

/**
 * An address editor for adding or updating an address.
 *
 * @param binding The binding used to display the view.
 * @param interactor [AddressEditorInteractor] used to respond to any user interactions.
 * @param region If the [RegionState] is available, it will be used to set the country when adding a new address.
 * @param address An [Address] to edit.
 */
class AddressEditorView(
    private val binding: FragmentAddressEditorBinding,
    private val interactor: AddressEditorInteractor,
    private val region: RegionState? = RegionState.Default,
    private val address: Address? = null,
) {

    /**
     * Binds the view in the [AddressEditorFragment], using the current [Address] if available.
     */
    fun bind() {
        binding.nameInput.apply {
            requestFocus()
            placeCursorAtEnd()
            showKeyboard()
        }

        binding.cancelButton.setOnClickListener {
            interactor.onCancelButtonClicked()
        }

        binding.saveButton.setOnClickListener {
            saveAddress()
        }

        address?.let { address ->
            binding.emailInput.setText(address.email)
            binding.phoneInput.setText(address.tel)
            binding.nameInput.setText(address.name)
            binding.streetAddressInput.setText(address.streetAddress)
            binding.cityInput.setText(address.addressLevel2)
            binding.zipInput.setText(address.postalCode)

            binding.deleteButton.apply {
                isVisible = true
                setOnClickListener { view ->
                    showConfirmDeleteAddressDialog(view.context, address.guid)
                }
            }
        }

        bindDropdowns()
    }

    internal fun saveAddress() {
        binding.root.hideKeyboard()

        val addressFields = UpdatableAddressFields(
            name = binding.nameInput.text.toString(),
            organization = "",
            streetAddress = binding.streetAddressInput.text.toString(),
            addressLevel3 = "",
            addressLevel2 = binding.cityInput.text.toString(),
            addressLevel1 = binding.subregionDropDown.selectedItem.toString(),
            postalCode = binding.zipInput.text.toString(),
            country = binding.countryDropDown.selectedItem.toString().toCountryCode(),
            tel = binding.phoneInput.text.toString(),
            email = binding.emailInput.text.toString(),
        )

        if (address != null) {
            interactor.onUpdateAddress(address.guid, addressFields)
//            Addresses.updated.add()
        } else {
            interactor.onSaveAddress(addressFields)
//            Addresses.saved.add()
        }
    }

    internal fun showConfirmDeleteAddressDialog(context: Context, guid: String) {
        AlertDialog.Builder(context).apply {
            setMessage(R.string.addressess_confirm_dialog_message_2)
            setNegativeButton(R.string.addressess_confirm_dialog_cancel_button) { dialog: DialogInterface, _ ->
                dialog.cancel()
            }
            setPositiveButton(R.string.addressess_confirm_dialog_ok_button) { _, _ ->
                interactor.onDeleteAddress(guid)
//                Addresses.deleted.add()
            }
            create().withCenterAlignedButtons()
        }.show()
    }

    @SuppressLint("VisibleForTests")
    private fun bindDropdowns() {
        val adapter = ArrayAdapter(
            binding.root.context,
            android.R.layout.simple_spinner_dropdown_item,
            countries.values.map { it.displayName },
        )

        val selectedCountryKey = (address?.country ?: region?.home).takeIf {
            it in countries.keys
        } ?: DEFAULT_COUNTRY

        val selectedPosition = countries.values
            .indexOfFirst { it.countryCode == selectedCountryKey }
            .takeIf { it > 0 }
            ?: 0

        binding.countryDropDown.adapter = adapter
        binding.countryDropDown.setSelection(selectedPosition)
        binding.countryDropDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val newCountryKey = binding.countryDropDown.selectedItem.toString().toCountryCode()
                countries[newCountryKey]?.let { country ->
                    bindSubregionDropdown(country)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        }

        countries[selectedCountryKey]?.let { country ->
            bindSubregionDropdown(country)
        }
    }

    private fun bindSubregionDropdown(@SuppressLint("VisibleForTests") country: Country) {
        val subregions = country.subregions
        val selectedSubregion = address?.addressLevel1?.takeIf { it in subregions }
            ?: subregions.first()

        val adapter = ArrayAdapter(
            binding.root.context,
            android.R.layout.simple_spinner_dropdown_item,
            country.subregions,
        )

        val selectedPosition = subregions.indexOf(selectedSubregion).takeIf { it > 0 } ?: 0
        binding.subregionDropDown.adapter = adapter
        binding.subregionDropDown.setSelection(selectedPosition)
        binding.subregionTitle.setText(country.subregionTitleResource)
    }
}
