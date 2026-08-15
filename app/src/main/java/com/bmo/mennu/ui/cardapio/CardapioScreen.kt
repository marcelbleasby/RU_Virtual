package com.bmo.mennu.ui.cardapio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import android.widget.Toast
import com.bmo.mennu.ui.components.AppHeader
import com.bmo.mennu.ui.components.PillVariant
import com.bmo.mennu.ui.components.SelectionPill
import com.bmo.mennu.ui.navigation.Screen

@Composable
fun CardapioScreen(navController: NavHostController, viewModel: CardapioViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.errorMessageShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader(
            userName = uiState.nomeExibicao,
            onAvatarClick = {
                viewModel.onLogoutClicked()
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cardápio Semanal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(20.dp))

            WeekNavigator(
                weekDays = uiState.weekDays,
                selectedDayIndex = uiState.selectedDayIndex,
                onDaySelected = viewModel::onDaySelected,
                onPreviousWeek = viewModel::onPreviousWeek,
                onNextWeek = viewModel::onNextWeek
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SelectionPill(
                        label = "Todos",
                        selected = uiState.selectedMealType == null,
                        variant = PillVariant.MEAL_TYPE,
                        onClick = { viewModel.onMealTypeSelected(null) }
                    )
                }
                items(uiState.availableMealTypes) { mealType ->
                    SelectionPill(
                        label = mealType.nome,
                        selected = uiState.selectedMealType?.id == mealType.id,
                        variant = PillVariant.MEAL_TYPE,
                        onClick = { viewModel.onMealTypeSelected(mealType) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            uiState.selectedDay?.let { day ->
                HighlightBanner(day = day, selectedMealType = uiState.selectedMealType)
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (uiState.visibleMealTypes.isEmpty()) {
                Text(
                    text = "Nenhum prato cadastrado para esta refeição.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.visibleMealTypes.forEach { mealType ->
                    Text(
                        text = mealType.nome,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val dishesByCategory = uiState.selectedDay?.meals?.get(mealType)
                        .orEmpty()
                        .groupBy { it.category }
                        .toSortedMap(compareBy { it.ordinal })

                    dishesByCategory.forEach { (category, dishes) ->
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        dishes.forEach { dish ->
                            DishCard(dish)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
