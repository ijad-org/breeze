package com.ijad.breeze.ui.legal

import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.R
import com.ijad.breeze.ui.components.BreezeAppBar
import com.ijad.breeze.ui.components.GlassCard
import com.ijad.breeze.ui.components.RowDivider
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.components.phoneWidth
import com.ijad.breeze.ui.theme.AutoTint
import com.ijad.breeze.ui.theme.ink

/** One shipped component and the licence it is used under; [text] is the full licence, bundled in res/raw. */
private class Notice(val name: String, val detail: String, val license: String, @RawRes val text: Int?)

/**
 * Everything in the release APK, from `releaseRuntimeClasspath` (test-only libraries don't ship).
 * Add an entry when adding a dependency with a different licence.
 */
private val Notices = listOf(
    Notice(
        "Breeze",
        "This app.",
        "MIT License",
        R.raw.license_mit
    ),
    Notice(
        "Plus Jakarta Sans",
        "The app's typeface. Copyright 2020 The Plus Jakarta Sans Project Authors (github.com/tokotype/PlusJakartaSans).",
        "SIL Open Font License 1.1",
        R.raw.license_ofl
    ),
    Notice(
        "Android Jetpack (AndroidX)",
        "Compose, Material 3, Material icons, Navigation, Lifecycle, Activity, Core, DataStore, ProfileInstaller and their AndroidX dependencies. Copyright The Android Open Source Project.",
        "Apache License 2.0",
        R.raw.license_apache
    ),
    Notice(
        "Kotlin and kotlinx.coroutines",
        "Kotlin standard library, coroutines and JetBrains annotations. Copyright JetBrains s.r.o. and Kotlin Programming Language contributors.",
        "Apache License 2.0",
        R.raw.license_apache
    ),
    Notice(
        "Okio and Guava ListenableFuture",
        "Used by DataStore and AndroidX. Copyright Square, Inc. and The Guava Authors.",
        "Apache License 2.0",
        R.raw.license_apache
    ),
    Notice(
        "IRremoteESP8266",
        "The IR protocol layouts and timings for the AC codecs follow this library (github.com/crankyoldgit/IRremoteESP8266).",
        "GNU LGPL 2.1",
        null
    )
)

/** Open-source licences, from Settings → About. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .breezeBackground(AutoTint)
            .phoneWidth()
    ) {
        BreezeAppBar(title = "Open-source licenses", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 44.dp)
        ) {
            Text(
                "Breeze is built with these open-source projects. Tap one to read its license.",
                style = MaterialTheme.typography.bodySmall,
                color = ink(0.45f),
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
            GlassCard(Modifier.fillMaxWidth()) {
                Notices.forEachIndexed { i, notice ->
                    NoticeRow(notice)
                    if (i < Notices.lastIndex) RowDivider()
                }
            }
        }
    }
}

@Composable
private fun NoticeRow(notice: Notice) {
    val context = LocalContext.current
    var open by rememberSaveable(notice.name) { mutableStateOf(false) }
    val text = remember(notice.text) {
        notice.text?.let { id -> context.resources.openRawResource(id).bufferedReader().use { it.readText() } }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (text != null) Modifier.clickable { open = !open } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(notice.name, style = MaterialTheme.typography.titleSmall, fontSize = 14.sp, color = ink(0.9f))
                Text(notice.license, style = MaterialTheme.typography.labelMedium, color = ink(0.55f), modifier = Modifier.padding(top = 2.dp))
            }
            if (text != null) {
                Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (open) "Hide license" else "Show license",
                    tint = ink(0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            notice.detail,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = ink(0.5f),
            modifier = Modifier.padding(top = 6.dp)
        )
        if (text != null) {
            AnimatedVisibility(visible = open) {
                // The files are hard-wrapped at about 70 columns; scroll sideways rather than re-wrap them.
                Text(
                    text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    softWrap = false,
                    color = ink(0.6f),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}
