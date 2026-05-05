package com.ecobook.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private val brazilianCities = listOf(
    "Sao Paulo",
    "Rio de Janeiro",
    "Belo Horizonte",
    "Curitiba",
    "Porto Alegre",
    "Florianopolis",
    "Sao Jose",
    "Joinville",
    "Blumenau",
    "Chapeco",
    "Criciuma",
    "Londrina",
    "Maringa",
    "Campinas",
    "Santos"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Cidade",
    isError: Boolean = false,
    supportingText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredCities = remember(value) {
        val query = value.trim().lowercase()
        brazilianCities.filter { city ->
            query.isBlank() || city.lowercase().contains(query)
        }.take(6)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredCities.isNotEmpty(),
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            isError = isError,
            singleLine = true,
            supportingText = supportingText?.let { message ->
                { Text(message) }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded && filteredCities.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filteredCities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city) },
                    onClick = {
                        onValueChange(city)
                        expanded = false
                    }
                )
            }
        }
    }
}
