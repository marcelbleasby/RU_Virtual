package com.bmo.mennu.ui.home

import com.bmo.mennu.data.model.TipoRefeicaoResponse

// Decide qual TipoRefeicao é "agora" — mesma regra do backend (MealAccessPolicy:
// horario_inicio <= now <= horario_fim, inclusive nas duas pontas). Se nenhuma
// janela contém o horário atual, cai pra próxima que ainda vai começar hoje.
object MealWindowResolver {
    fun resolveCurrentOrNext(nowMinutes: Int, tipos: List<TipoRefeicaoResponse>): TipoRefeicaoResponse? {
        val current = tipos.firstOrNull { tipo ->
            nowMinutes in tipo.horarioInicio.toMinutesOfDay()..tipo.horarioFim.toMinutesOfDay()
        }
        if (current != null) return current

        return tipos
            .filter { it.horarioInicio.toMinutesOfDay() > nowMinutes }
            .minByOrNull { it.horarioInicio.toMinutesOfDay() }
    }
}

// "HH:mm:ss" -> minutos desde 00:00.
private fun String.toMinutesOfDay(): Int {
    val partes = split(":")
    val hora = partes[0].toInt()
    val minuto = partes.getOrElse(1) { "0" }.toInt()
    return hora * 60 + minuto
}
