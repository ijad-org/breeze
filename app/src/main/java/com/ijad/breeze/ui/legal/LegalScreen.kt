package com.ijad.breeze.ui.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.ui.components.BreezeAppBar
import com.ijad.breeze.ui.components.GlassCard
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.theme.AutoTint
import com.ijad.breeze.ui.theme.ink

enum class LegalDoc(val title: String, val updated: String, val sections: List<Pair<String, String>>) {
    Privacy(
        "Privacy Policy",
        "Last updated October 2026",
        listOf(
            "The short version" to "Breeze doesn't collect, sell or share your data. It has no accounts, no ads, no analytics and no network access.",
            "What stays on your phone" to "The ACs you pair (name, brand, code number), each AC's last remote settings, any timers you set, and your Settings choices. These are stored only in the app's private storage on this device and are deleted when you uninstall Breeze or clear its data.",
            "Permissions" to "Infrared: sends commands to your AC through your phone's IR blaster. Vibration: a short tick when a command is sent. Alarms and notifications: only so timers you set can run and tell you when they did. Breeze doesn't use your location, camera, microphone, contacts or files.",
            "Backups" to "If Android backup is turned on, your Breeze settings may be included in your device backup, which is handled by your phone's backup service.",
            "Children" to "Breeze collects no personal information from anyone, including children.",
            "Changes" to "If this policy ever changes, the new version will ship with an app update and appear here."
        )
    ),
    Terms(
        "Terms of Service",
        "Last updated October 2026",
        listOf(
            "Using Breeze" to "Breeze is a free, open-source remote for air conditioners, provided under the MIT License. You may use it for any lawful purpose.",
            "No warranty" to "Breeze is provided \"as is\", without warranty of any kind. IR codes vary by model and may not work with your AC. Timers depend on your phone being on, nearby and pointed at the AC, and on Android allowing the alarm to run on time.",
            "Your responsibility" to "Use sensible temperature settings, and don't rely on Breeze where an AC failing to turn on or off could cause harm or damage.",
            "Trademarks" to "Breeze is not affiliated with, endorsed by or sponsored by Samsung, LG, Daikin, Mitsubishi, Voltas, Blue Star, Carrier, Haier, Panasonic or any other manufacturer. Brand names are only used to identify compatible devices.",
            "Liability" to "To the extent permitted by law, the authors are not liable for any claim, damages or other liability arising from use of the app."
        )
    )
}

@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .breezeBackground(AutoTint)
    ) {
        BreezeAppBar(title = doc.title, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 44.dp)
        ) {
            Text(doc.updated, style = MaterialTheme.typography.bodySmall, color = ink(0.45f), modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    doc.sections.forEachIndexed { i, (heading, body) ->
                        Text(
                            heading,
                            style = MaterialTheme.typography.titleSmall,
                            color = ink(0.9f),
                            modifier = Modifier.padding(top = if (i == 0) 0.dp else 18.dp, bottom = 6.dp)
                        )
                        Text(body, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp, lineHeight = 21.sp, color = ink(0.65f))
                    }
                }
            }
        }
    }
}
