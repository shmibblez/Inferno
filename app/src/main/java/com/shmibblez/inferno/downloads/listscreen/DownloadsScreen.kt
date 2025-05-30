/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.downloads.listscreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.lib.state.ext.observeAsState
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.list.SelectableListItem
import com.shmibblez.inferno.compose.snackbar.AcornSnackbarHostState
import com.shmibblez.inferno.compose.snackbar.SnackbarHost
import com.shmibblez.inferno.compose.snackbar.SnackbarState
import com.shmibblez.inferno.ext.getIcon
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * Downloads screen that displays the list of downloads.
 *
 * @param downloadsStore The [DownloadFragmentStore] used to manage and access the state of download items.
 * @param onItemClick Invoked when a download item is clicked.
 * @param onItemDeleteClick Invoked when delete icon button is clicked.
 */
@Composable
fun DownloadsScreen(
    downloadsStore: DownloadFragmentStore,
    onItemClick: (DownloadItem) -> Unit,
    onItemDeleteClick: (DownloadItem) -> Unit,
) {
    val uiState by downloadsStore.observeAsState(initialValue = downloadsStore.state) { it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FirefoxTheme.colors.layer1),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.isEmptyState) {
            NoDownloadsText()
        } else {
            DownloadsContent(
                state = uiState,
                onClick = onItemClick,
                onSelectionChange = { item, isSelected ->
                    if (isSelected) {
                        downloadsStore.dispatch(DownloadFragmentAction.AddItemForRemoval(item))
                    } else {
                        downloadsStore.dispatch(DownloadFragmentAction.RemoveItemForRemoval(item))
                    }
                },
                onDeleteClick = onItemDeleteClick,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = FirefoxTheme.size.containerMaxWidth),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DownloadsContent(
    state: DownloadFragmentState,
    onClick: (DownloadItem) -> Unit,
    onSelectionChange: (DownloadItem, Boolean) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier,
    ) {
        items(
            items = state.itemsToDisplay,
            key = { it.id },
        ) { downloadItem ->
            SelectableListItem(
                label = downloadItem.fileName ?: downloadItem.url,
                description = downloadItem.formattedSize,
                isSelected = state.mode.selectedItems.contains(downloadItem),
                icon = downloadItem.getIcon(),
                afterListAction = {
                    if (state.isNormalMode) {
                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { onDeleteClick(downloadItem) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.mozac_ic_delete_24),
                                contentDescription = stringResource(id = R.string.download_delete_item_1),
                                tint = FirefoxTheme.colors.iconPrimary,
                            )
                        }
                    }
                },
                modifier = Modifier
                    .animateItem()
                    .combinedClickable(
                        onClick = {
                            if (state.isNormalMode) {
                                onClick(downloadItem)
                            } else {
                                onSelectionChange(
                                    downloadItem,
                                    !state.mode.selectedItems.contains(downloadItem),
                                )
                            }
                        },
                        onLongClick = {
                            if (state.isNormalMode) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectionChange(downloadItem, true)
                            }
                        },
                    )
                    .testTag("${DownloadsListTestTag.DOWNLOADS_LIST_ITEM}.${downloadItem.fileName}"),
            )
        }
    }
}

@Composable
private fun NoDownloadsText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.download_empty_message_1),
        modifier = modifier,
        color = FirefoxTheme.colors.textSecondary,
        style = FirefoxTheme.typography.body1,
    )
}

private class DownloadsScreenPreviewModelParameterProvider :
    PreviewParameterProvider<DownloadFragmentState> {
    override val values: Sequence<DownloadFragmentState>
        get() = sequenceOf(
            DownloadFragmentState.INITIAL,
            DownloadFragmentState(
                items = listOf(
                    DownloadItem(
                        id = "1",
                        fileName = "File 1",
                        url = "https://example.com/file1",
                        formattedSize = "1.2 MB",
                        contentType = "application/pdf",
                        status = DownloadState.Status.COMPLETED,
                        filePath = "/path/to/file1",
                    ),
                    DownloadItem(
                        id = "2",
                        fileName = "File 2",
                        url = "https://example.com/file2",
                        formattedSize = "2.3 MB",
                        contentType = "image/png",
                        status = DownloadState.Status.COMPLETED,
                        filePath = "/path/to/file1",
                    ),
                    DownloadItem(
                        id = "3",
                        fileName = "File 3",
                        url = "https://example.com/file3",
                        formattedSize = "3.4 MB",
                        contentType = "application/zip",
                        status = DownloadState.Status.COMPLETED,
                        filePath = "/path/to/file1",
                    ),
                ),
                mode = DownloadFragmentState.Mode.Normal,
                pendingDeletionIds = emptySet(),
                isDeletingItems = false,
            ),
        )
}

@Composable
@FlexibleWindowLightDarkPreview
private fun DownloadsScreenPreviews(
    @PreviewParameter(DownloadsScreenPreviewModelParameterProvider::class) state: DownloadFragmentState,
) {
    val store = remember { DownloadFragmentStore(initialState = state) }
    val snackbarHostState = remember { AcornSnackbarHostState() }
    val scope = rememberCoroutineScope()
    FirefoxTheme {
        Box {
            DownloadsScreen(
                downloadsStore = store,
                onItemClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            SnackbarState(message = "Item ${it.fileName} clicked"),
                        )
                    }
                },
                onItemDeleteClick = {
                    store.dispatch(DownloadFragmentAction.UpdateDownloadItems(store.state.items - it))
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            SnackbarState(
                                message = "Item ${it.fileName} deleted",
                                type = SnackbarState.Type.Warning,
                            ),
                        )
                    }
                },
            )
            SnackbarHost(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
