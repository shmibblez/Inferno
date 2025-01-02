/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.logins.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputLayout
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.telemetry.glean.private.NoExtras
import com.shmibblez.inferno.AuthenticationStatus
import com.shmibblez.inferno.BiometricAuthenticationManager
import com.shmibblez.inferno.GleanMetrics.Logins
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.StoreProvider
import com.shmibblez.inferno.databinding.FragmentEditLoginBinding
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.registerForActivityResult
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.toEditable
import com.shmibblez.inferno.settings.biometric.bindBiometricsCredentialsPromptOrShowWarning
import com.shmibblez.inferno.settings.logins.LoginsAction
import com.shmibblez.inferno.settings.logins.LoginsFragmentStore
import com.shmibblez.inferno.settings.logins.SavedLogin
import com.shmibblez.inferno.settings.logins.controller.SavedLoginsStorageController
import com.shmibblez.inferno.settings.logins.createInitialLoginsListState
import com.shmibblez.inferno.settings.logins.interactor.EditLoginInteractor
import com.shmibblez.inferno.settings.logins.togglePasswordReveal

/**
 * Displays the editable saved login information for a single website
 */
@Suppress("TooManyFunctions", "NestedBlockDepth", "ForbiddenComment")
class EditLoginFragment : Fragment(R.layout.fragment_edit_login), MenuProvider {

    private val args by navArgs<EditLoginFragmentArgs>()
    private lateinit var loginsFragmentStore: LoginsFragmentStore
    private lateinit var interactor: EditLoginInteractor
    private lateinit var oldLogin: SavedLogin

    private var duplicateLogin: SavedLogin? = null

    private var usernameChanged = false
    private var passwordChanged = false

    private var validPassword = true
    private var validUsername = true

    private var _binding: FragmentEditLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var startForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForResult = registerForActivityResult {
            BiometricAuthenticationManager.biometricAuthenticationNeededInfo.shouldShowAuthenticationPrompt =
                false
            BiometricAuthenticationManager.biometricAuthenticationNeededInfo.authenticationStatus =
                AuthenticationStatus.AUTHENTICATED
            setSecureContentVisibility(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        _binding = FragmentEditLoginBinding.bind(view)

        oldLogin = args.savedLoginItem

        loginsFragmentStore =
            StoreProvider.get(findNavController().getBackStackEntry(R.id.savedLogins)) {
                LoginsFragmentStore(
                    createInitialLoginsListState(requireContext().settings()),
                )
            }

        interactor = EditLoginInteractor(
            SavedLoginsStorageController(
                passwordsStorage = requireContext().components.core.passwordsStorage,
                lifecycleScope = lifecycleScope,
                navController = findNavController(),
                loginsFragmentStore = loginsFragmentStore,
                clipboardHandler = requireContext().components.clipboardHandler,
            ),
        )

        loginsFragmentStore.dispatch(LoginsAction.UpdateCurrentLogin(args.savedLoginItem))

        // initialize editable values
        binding.hostnameText.text = args.savedLoginItem.origin.toEditable()
        binding.usernameText.text = args.savedLoginItem.username.toEditable()
        binding.passwordText.text = args.savedLoginItem.password.toEditable()

        binding.clearUsernameTextButton.isEnabled = oldLogin.username.isNotEmpty()

        formatEditableValues()
        setUpClickListeners()
        setUpTextListeners()
        togglePasswordReveal(binding.passwordText, binding.revealPasswordButton)
        findDuplicate()

        consumeFrom(loginsFragmentStore) {
            duplicateLogin = loginsFragmentStore.state.duplicateLogin
            updateUsernameField()
        }
    }

    private fun formatEditableValues() {
        binding.hostnameText.isClickable = false
        binding.hostnameText.isFocusable = false
        binding.usernameText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        // TODO: extend PasswordTransformationMethod() to change bullets to asterisks
        binding.passwordText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.passwordText.compoundDrawablePadding =
            requireContext().resources
                .getDimensionPixelOffset(R.dimen.saved_logins_end_icon_drawable_padding)
    }

    private fun setUpClickListeners() {
        binding.clearUsernameTextButton.setOnClickListener {
            binding.usernameText.text?.clear()
            binding.usernameText.isCursorVisible = true
            binding.usernameText.hasFocus()
            binding.inputLayoutUsername.hasFocus()
            it.isEnabled = false
        }
        binding.clearPasswordTextButton.setOnClickListener {
            binding.passwordText.text?.clear()
            binding.passwordText.isCursorVisible = true
            binding.passwordText.hasFocus()
            binding.inputLayoutPassword.hasFocus()
        }
        binding.revealPasswordButton.setOnClickListener {
            togglePasswordReveal(binding.passwordText, binding.revealPasswordButton)
        }
    }

    private fun setUpTextListeners() {
        val frag = view?.findViewById<View>(R.id.editLoginFragment)
        frag?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view?.hideKeyboard()
            }
        }
        binding.editLoginLayout.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                view?.hideKeyboard()
            }
        }

        binding.usernameText.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(u: Editable?) {
                    when {
                        u.toString().isEmpty() -> {
                            validUsername = false
                            binding.clearUsernameTextButton.isVisible = false
                            setLayoutError(
                                context?.getString(R.string.saved_login_username_required_2),
                                binding.inputLayoutUsername,
                            )
                        }

                        else -> {
                            validUsername = true
                            binding.inputLayoutUsername.error = null
                            binding.inputLayoutUsername.errorIconDrawable = null
                            binding.inputLayoutUsername.isVisible = true
                            binding.clearUsernameTextButton.isVisible = true
                        }
                    }
                    updateUsernameField()
                    findDuplicate()
                    setSaveButtonState()
                }

                override fun beforeTextChanged(
                    u: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                    // NOOP
                }

                override fun onTextChanged(u: CharSequence?, start: Int, before: Int, count: Int) {
                    // NOOP
                }
            },
        )

        binding.passwordText.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(p: Editable?) {
                    when {
                        p.toString().isEmpty() -> {
                            validPassword = false
                            passwordChanged = true
                            binding.revealPasswordButton.isVisible = false
                            binding.clearPasswordTextButton.isVisible = false
                            setLayoutError(
                                context?.getString(R.string.saved_login_password_required_2),
                                binding.inputLayoutPassword,
                            )
                        }

                        p.toString() == oldLogin.password -> {
                            passwordChanged = false
                            validPassword = true
                            binding.inputLayoutPassword.error = null
                            binding.inputLayoutPassword.errorIconDrawable = null
                            binding.revealPasswordButton.isVisible = true
                            binding.clearPasswordTextButton.isVisible = true
                        }
                        else -> {
                            passwordChanged = true
                            validPassword = true
                            binding.inputLayoutPassword.error = null
                            binding.inputLayoutPassword.errorIconDrawable = null
                            binding.revealPasswordButton.isVisible = true
                            binding.clearPasswordTextButton.isVisible = true
                        }
                    }
                    setSaveButtonState()
                }

                override fun beforeTextChanged(
                    p: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                    // NOOP
                }

                override fun onTextChanged(p: CharSequence?, start: Int, before: Int, count: Int) {
                    // NOOP
                }
            },
        )
    }

    private fun findDuplicate() {
        interactor.findDuplicate(
            oldLogin.guid,
            binding.usernameText.text.toString(),
            binding.passwordText.text.toString(),
        )
    }

    private fun updateUsernameField() {
        val currentValue = binding.usernameText.text.toString()
        val layout = binding.inputLayoutUsername
        val clearButton = binding.clearUsernameTextButton
        when {
            (duplicateLogin == null || oldLogin.username == currentValue) -> {
                // Valid login, either because there's no dupe or because the
                // existing login was already a dupe and the username hasn't
                // changed
                usernameChanged = oldLogin.username != currentValue
            }
            else -> {
                // Invalid login because it's a dupe of another one
                usernameChanged = true
                validUsername = false
                layout.error = context?.getString(R.string.saved_login_duplicate)
                layout.setErrorIconDrawable(R.drawable.mozac_ic_warning_with_bottom_padding)
                layout.setErrorIconTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.fx_mobile_text_color_critical,
                        ),
                    ),
                )
                clearButton.isVisible = false
            }
        }
        clearButton.isEnabled = currentValue.isNotEmpty()
        setSaveButtonState()
    }

    private fun setLayoutError(error: String?, inputLayout: TextInputLayout) {
        inputLayout.let { layout ->
            layout.error = error
            layout.setErrorIconDrawable(R.drawable.mozac_ic_warning_with_bottom_padding)
            layout.setErrorIconTintList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.fx_mobile_text_color_critical),
                ),
            )
        }
    }

    private fun setSaveButtonState() {
        activity?.invalidateOptionsMenu()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        val saveButton = menu.findItem(R.id.save_login_button)
        val changesMadeWithNoErrors =
            validUsername && validPassword && (usernameChanged || passwordChanged)
        saveButton.isEnabled =
            changesMadeWithNoErrors // don't enable saving until something has been changed
    }

    override fun onResume() {
        super.onResume()
        if (BiometricAuthenticationManager.biometricAuthenticationNeededInfo.shouldShowAuthenticationPrompt) {
            BiometricAuthenticationManager.biometricAuthenticationNeededInfo.shouldShowAuthenticationPrompt =
                false
            BiometricAuthenticationManager.biometricAuthenticationNeededInfo.authenticationStatus =
                AuthenticationStatus.AUTHENTICATION_IN_PROGRESS
            setSecureContentVisibility(false)

            bindBiometricsCredentialsPromptOrShowWarning(
                view = requireView(),
                onShowPinVerification = { intent -> startForResult.launch(intent) },
                onAuthSuccess = {
                    BiometricAuthenticationManager.biometricAuthenticationNeededInfo.authenticationStatus =
                        AuthenticationStatus.AUTHENTICATED
                    setSecureContentVisibility(true)
                },
                onAuthFailure = {
                    BiometricAuthenticationManager.biometricAuthenticationNeededInfo.authenticationStatus =
                        AuthenticationStatus.NOT_AUTHENTICATED
                    setSecureContentVisibility(false)
                },
            )
        } else {
            setSecureContentVisibility(
                BiometricAuthenticationManager.biometricAuthenticationNeededInfo.authenticationStatus ==
                    AuthenticationStatus.AUTHENTICATED,
            )
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            view?.hideKeyboard()
            interactor.onSaveLogin(
                args.savedLoginItem.guid,
                binding.usernameText.text.toString(),
                binding.passwordText.text.toString(),
            )
            Logins.saveEditedLogin.record(NoExtras())
            Logins.modified.add()
            true
        }
        else -> false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // If you've made it here and you're authenticated, let's reset the values so we don't
        // prompt the user again when navigating back.
        val authenticated = BiometricAuthenticationManager.biometricAuthenticationNeededInfo.authenticationStatus ==
            AuthenticationStatus.AUTHENTICATED
        BiometricAuthenticationManager.biometricAuthenticationNeededInfo.shouldShowAuthenticationPrompt =
            !authenticated
    }

    private fun setSecureContentVisibility(isVisible: Boolean) {
        binding.editLoginLayout.isVisible = isVisible
    }
}