package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.octestx.basic.multiplatform.ui.ui.global.Language
import io.github.octestx.basic.multiplatform.ui.ui.global.LanguageRepository

private val availableLanguages = listOf(Language.English, Language.ChineseSimplified, Language.ChineseTraditional, Language.Japanese)
@Composable
fun SelectLanguage(nextStep: () -> Unit) {
    Column(Modifier.padding(25.dp)) {
        Text("请选择语言[未完成，选哪个都是简体中文]", style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.fillMaxWidth()) {
            items(availableLanguages) { language ->
                Card(onClick = {
                    LanguageRepository.switchLanguage(language)
                    nextStep()
                }, modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(language.text)
                    }
                }
            }
        }
    }
}