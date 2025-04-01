package com.shmibblez.inferno.browser.prompts.download.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.R
import com.shmibblez.inferno.browser.prompts.PromptBottomSheetTemplate
import com.shmibblez.inferno.browser.prompts.PromptBottomSheetTemplateAction
import com.shmibblez.inferno.browser.prompts.PromptBottomSheetTemplateButtonPosition
import com.shmibblez.inferno.compose.base.InfernoText
import mozilla.components.browser.state.state.content.DownloadState

@Composable
fun DownloadPrompt(
    download: DownloadState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    PromptBottomSheetTemplate(
        onDismissRequest = onCancelDownload,
        negativeAction = PromptBottomSheetTemplateAction(
            text = stringResource(android.R.string.cancel),
            action = {
                onCancelDownload.invoke()
            },
        ),
        positiveAction = PromptBottomSheetTemplateAction(
            text = stringResource(R.string.mozac_feature_downloads_dialog_download),
            action = {
                onStartDownload.invoke()
            },
        ),
        buttonPosition = PromptBottomSheetTemplateButtonPosition.BOTTOM,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.mozac_feature_download_ic_download_complete),
                contentDescription = "download complete icon",
                modifier = Modifier.size(32.dp),
                tint = Color.White,
            )
            InfernoText(
                text = stringResource(R.string.mozac_feature_downloads_dialog_title2),
                fontColor = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1F),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        Text(
            text = download.fileName ?: "",
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(1F)
                .padding(horizontal = 16.dp)
                // padding is icon + row item spacing
                .padding(start = 32.dp + 8.dp)
        )
    }

}