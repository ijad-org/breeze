package com.ijad.breeze.ui.brand

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.data.AcBrand
import com.ijad.breeze.data.Brands
import com.ijad.breeze.ui.components.BreezeAppBar
import com.ijad.breeze.ui.components.GlassCard
import com.ijad.breeze.ui.components.RoundButton
import com.ijad.breeze.ui.components.RowDivider
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.components.glass
import com.ijad.breeze.ui.theme.AutoTint
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.LocalBreezeDark
import com.ijad.breeze.ui.theme.ink

/** Choose your AC brand — docs/design/02-brand-select.png */
@Composable
fun BrandSelectScreen(
    onBack: () -> Unit,
    onContinue: (AcBrand) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val filtered = remember(query) {
        Brands.all.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    }
    val selected = Brands.byId(selectedId.orEmpty())
    val tint = AutoTint

    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(tint)
            .navigationBarsPadding()
            .imePadding()
    ) {
        BreezeAppBar(title = "Choose your AC brand", onBack = onBack, modifier = Modifier.padding(top = 4.dp))

        SearchField(
            query = query,
            onQuery = { query = it },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (filtered.isEmpty()) {
                Text(
                    "No brands match \"$query\". Try \"Other\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink(0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(4.dp))
                    filtered.forEachIndexed { i, brand ->
                        BrandRow(
                            brand = brand,
                            selected = brand.id == selectedId,
                            tint = tint,
                            onClick = { selectedId = brand.id }
                        )
                        if (i < filtered.lastIndex) RowDivider()
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        RoundButton(
            text = "Continue",
            onClick = { selected?.let(onContinue) },
            enabled = selected != null,
            tint = CoolTint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .glass(shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = ink(0.45f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Search brands…", style = MaterialTheme.typography.bodyMedium, color = ink(0.4f))
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink()),
                cursorBrush = SolidColor(AutoTint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BrandRow(brand: AcBrand, selected: Boolean, tint: Color, onClick: () -> Unit) {
    val dark = LocalBreezeDark.current
    val rowBg by animateColorAsState(if (selected) tint.copy(alpha = 0.12f) else Color.Transparent, tween(180), label = "row")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selected) tint.copy(alpha = 0.22f)
                    else if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                    CircleShape
                )
                .border(1.5.dp, if (selected) tint.copy(alpha = 0.4f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                brand.letter.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) tint else ink(0.6f)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            brand.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) tint else ink(0.85f)
        )
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = tint, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ink(0.3f), modifier = Modifier.size(20.dp))
        }
    }
}
