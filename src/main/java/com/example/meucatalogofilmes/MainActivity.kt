package com.example.meucatalogofilmes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable data object Splash : AppRoute
    @Serializable data object Login : AppRoute
    @Serializable data object Home : AppRoute
    @Serializable data object Favorites : AppRoute
    @Serializable data object Profile : AppRoute
    @Serializable data object About : AppRoute
    @Serializable data class MovieDetail(val movieId: Int) : AppRoute
}

data class Movie(val id: Int, val title: String, val genre: String, val year: Int, val description: String)

class MovieViewModel : ViewModel() {
    val movies = listOf(
        Movie(1, "Interestelar", "Ficção científica", 2014, "Uma viagem pelo espaço em busca de um novo lar para a humanidade."),
        Movie(2, "O Batman", "Ação", 2022, "Batman investiga uma série de crimes misteriosos em Gotham."),
        Movie(3, "Divertida Mente", "Animação", 2015, "As emoções de uma garota aprendem a trabalhar juntas durante uma grande mudança."),
        Movie(4, "Homem-Aranha", "Aventura", 2018, "Miles Morales descobre seus poderes e aprende o que significa ser herói.")
    )

    var loggedIn by mutableStateOf(false)
        private set

    fun login() {
        loggedIn = true
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CineFavoritosTheme {
                val movieViewModel: MovieViewModel = viewModel()
                CineApp(movieViewModel)
            }
        }
    }
}

@Composable
fun CineApp(movieViewModel: MovieViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash
    ) {
        composable<AppRoute.Splash> {
            SplashScreen()
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200)
                val destino: AppRoute = if (movieViewModel.loggedIn) AppRoute.Home else AppRoute.Login
                navController.navigate(destino) {
                    popUpTo<AppRoute.Splash> { inclusive = true }
                }
            }
        }

        composable<AppRoute.Login> {
            LoginScreen {
                movieViewModel.login()
                navController.navigate(AppRoute.Home) {
                    popUpTo<AppRoute.Login> { inclusive = true }
                }
            }
        }

        composable<AppRoute.Home> {
            MainShell(navController, movieViewModel)
        }
    }
}

@Composable
fun MainShell(navController: NavHostController, vm: MovieViewModel) {
    val tabs = listOf(
        Triple(AppRoute.Home, "Filmes", Icons.Default.Movie),
        Triple(AppRoute.Favorites, "Favoritos", Icons.Default.Favorite),
        Triple(AppRoute.Profile, "Perfil", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (route, label, icon) ->
                    val selected = navController.currentDestination?.route?.contains(route::class.simpleName ?: "") == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo<AppRoute.Home> { saveState = true }
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        val current = navController.currentDestination?.route ?: ""
        Box(Modifier.padding(padding)) {
            when {
                current.contains("Favorites") -> FavoritesScreen(navController, vm)
                current.contains("Profile") -> ProfileScreen()
                current.contains("MovieDetail") -> {
                    val entry = navController.currentBackStackEntry
                    val route = entry?.toRoute<AppRoute.MovieDetail>()
                    if (route != null) MovieDetailScreen(route.movieId, vm, navController)
                }
                else -> HomeScreen(navController, vm)
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Movie, null, Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("CineFavoritos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Seus filmes, do seu jeito.")
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bem-vindo!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Entre para acessar seu catálogo de filmes.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogin, Modifier.fillMaxWidth()) {
            Text("Entrar")
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController, vm: MovieViewModel) {
    if (vm.movies.isEmpty()) {
        EmptyState("Nenhum filme cadastrado.")
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Meus filmes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Escolha um filme para ver os detalhes.")
        }
        items(vm.movies) { movie ->
            ElevatedCard(
                onClick = { navController.navigate(AppRoute.MovieDetail(movie.id)) },
                Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(movie.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${movie.genre} • ${movie.year}")
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(navController: NavHostController, vm: MovieViewModel) {
    val favorites = vm.movies.take(2)
    if (favorites.isEmpty()) {
        EmptyState("Você ainda não tem favoritos.")
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Favoritos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        items(favorites) { movie ->
            ListItem(
                headlineContent = { Text(movie.title) },
                supportingContent = { Text("${movie.genre} • ${movie.year}") },
                leadingContent = { Icon(Icons.Default.Favorite, contentDescription = "Filme favorito") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Icon(Icons.Default.Person, "Perfil", Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Meu perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Catálogo pessoal de filmes.")
    }
}

@Composable
fun MovieDetailScreen(movieId: Int, vm: MovieViewModel, navController: NavHostController) {
    val movie = vm.movies.firstOrNull { it.id == movieId }
    if (movie == null) {
        EmptyState("Filme não encontrado.")
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.Home, contentDescription = "Voltar")
        }
        Text(movie.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${movie.genre} • ${movie.year}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        Text(movie.description, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CineFavoritosTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF5B2C83),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = androidx.compose.ui.graphics.Color(0xFFE59F71),
        background = androidx.compose.ui.graphics.Color(0xFFFFF8F3),
        surface = androidx.compose.ui.graphics.Color(0xFFFFF8F3),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1E2D8)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
