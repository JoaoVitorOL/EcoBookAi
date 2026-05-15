package com.ecobook.location

import java.text.Normalizer
import java.util.Locale

object SouthBrazilCityCatalog {
    val cities = listOf(
        "Florianopolis",
        "Sao Jose",
        "Palhoca",
        "Biguacu",
        "Joinville",
        "Blumenau",
        "Itajai",
        "Balneario Camboriu",
        "Brusque",
        "Jaragua do Sul",
        "Chapeco",
        "Criciuma",
        "Lages",
        "Tubarao",
        "Rio do Sul",
        "Concordia",
        "Sao Bento do Sul",
        "Mafra",
        "Canoinhas",
        "Ararangua",
        "Curitiba",
        "Sao Jose dos Pinhais",
        "Pinhais",
        "Colombo",
        "Araucaria",
        "Londrina",
        "Maringa",
        "Cascavel",
        "Foz do Iguacu",
        "Ponta Grossa",
        "Guarapuava",
        "Paranagua",
        "Toledo",
        "Apucarana",
        "Campo Mourao",
        "Porto Alegre",
        "Canoas",
        "Novo Hamburgo",
        "Sao Leopoldo",
        "Caxias do Sul",
        "Pelotas",
        "Santa Maria",
        "Passo Fundo",
        "Bento Goncalves",
        "Rio Grande",
        "Gravatai",
        "Alvorada",
        "Viamao"
    )

    private val normalizedCities = cities.associateBy(::normalize)

    fun isSupported(value: String): Boolean {
        return normalizedCities.containsKey(normalize(value))
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase(Locale.ROOT)
    }
}
