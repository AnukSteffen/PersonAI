package com.example.personai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.personai.data.local.LocalLLMEngine
import com.example.personai.data.local.UserStore
import com.example.personai.ui.component.MainViewModel
import com.example.personai.ui.auth.AccountSwitchScreen
import com.example.personai.ui.auth.LoginScreen
import com.example.personai.ui.auth.RegisterScreen
import com.example.personai.ui.chat.ChatScreen
import com.example.personai.ui.contact.ContactScreen
import com.example.personai.ui.create.CreateWizardScreen
import com.example.personai.ui.discovery.DiscoveryScreen
import com.example.personai.ui.feed.FeedScreen
import com.example.personai.ui.feed.ForwardSelectScreen
import com.example.personai.ui.post.CreatePostScreen
import com.example.personai.ui.post.PostDetailScreen
import com.example.personai.ui.profile.NotificationScreen
import com.example.personai.ui.profile.PersonaProfileScreen
import com.example.personai.ui.profile.ProfileScreen
import com.example.personai.ui.profile.RelationshipScreen
import com.example.personai.ui.theme.PersonAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userStore: UserStore
    @Inject
    lateinit var localLLM: LocalLLMEngine

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        var startDestination by mutableStateOf<String?>(null)

        // 监听登录状态
        lifecycleScope.launch {
            userStore.userId.collect { id ->
                startDestination = if (id == null) "login_graph" else "main_app"
            }
        }

        lifecycleScope.launch {
            userStore.userId.collect { userId ->
                if (userId != null) {
                    Log.d("App", "用户已登录 ($userId)，开始后台预加载模型...")
                    // 在 IO 线程启动初始化，不阻塞 UI
                    launch(Dispatchers.IO) {
                        val success = localLLM.initialize()
                        if (!success) {
                            Log.w("App", "后台预加载失败，可能文件不存在")
                        }
                    }
                }
            }
        }

        setContent {
            val mainViewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val currentTheme by mainViewModel.themeMode.collectAsState()
            PersonAITheme(themeMode = currentTheme) {
                // 根据状态显示不同的入口
                if (startDestination == "login_graph") {
                    AuthNavigation(onLoginSuccess = {
                    })
                } else if (startDestination == "main_app") {
                    MainApp() // 进入主程序
                }
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Feed : BottomNavItem("feed", "广场", Icons.Default.DynamicFeed)
    object Discovery : BottomNavItem("discovery", "发现", Icons.Default.Explore)
    object Create : BottomNavItem("create", "创作", Icons.Default.AddCircle)
    object Messages : BottomNavItem("messages", "消息", Icons.Default.ChatBubble)
    object Profile : BottomNavItem("profile", "我的", Icons.Default.Person)
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 定义显示的顺序：广场 -> 发现 -> 创作 -> 消息 -> 我的
    val navItems = listOf(
        BottomNavItem.Feed,
        BottomNavItem.Discovery,
        BottomNavItem.Create,
        BottomNavItem.Messages,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            val showBottomBar = currentRoute == BottomNavItem.Feed.route ||
                    currentRoute == BottomNavItem.Discovery.route ||
                    currentRoute == BottomNavItem.Messages.route ||
                    currentRoute == BottomNavItem.Profile.route

            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .height(100.dp)
                        .navigationBarsPadding()
                ) {
                    //防止无限入栈
                    fun navigateToTab(route: String) {
                        navController.navigate(route) {
                            // 点击 Tab 时，弹出到起始页，保留状态
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // 避免重复点击同一个 Tab 产生多份实例
                            launchSingleTop = true
                            // 恢复之前的状态
                            restoreState = true
                        }
                    }

                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    // 选中时高亮
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                if (isSelected) return@NavigationBarItem
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("feed") { popUpTo(0) }
                    },
                    onGoToRegister = { navController.navigate("register") },
                    onNavigateToSwitchAccount = {
                        if (!navController.popBackStack("account_switch", inclusive = false)) {
                            navController.navigate("account_switch")
                        }
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = { phone, pass ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("registrant_info", listOf(phone, pass))
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 1. 广场页
            composable("feed") { backStackEntry ->
                val viewModel: com.example.personai.ui.feed.FeedViewModel =
                    androidx.hilt.navigation.compose.hiltViewModel()
                LaunchedEffect(Unit) {
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    val isFromDetail = previousRoute?.startsWith("post_detail") == true

                    if (!isFromDetail) {
                        viewModel.onSearch("") // 清空搜索词
                    }
                }

                FeedScreen(
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    },
                    onCreatePostClick = {
                        navController.navigate("create_post")
                    },
                    onNavigateToForward = { postId ->
                        navController.navigate("forward_select/$postId")
                    }
                )
            }

            // 2. 发现页
            composable("discovery") {
                DiscoveryScreen(
                    onPersonaClick = { personaId ->
                        navController.navigate("persona_profile/$personaId")
                    }
                )
            }

            // 3. 创作页
            composable("create") {
                CreateWizardScreen(
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    onSuccessClick = { newId ->
                        navController.navigate("persona_profile/$newId") {
                            popUpTo("create") { inclusive = true }
                        }
                    }
                )
            }

            // 4. 消息页
            composable("messages") {
                ContactScreen(
                    onChatClick = { personaId ->
                        navController.navigate("chat/$personaId")
                    }
                )
            }

            // 5. 我的页
            composable("profile") {
                ProfileScreen(
                    onPersonaClick = { personaId ->
                        // 跳转到角色详情页
                        navController.navigate("persona_profile/$personaId")
                    },
                    // 跳转到帖子详情
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    },
                    // 跳转到通知页
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSwitchAccount = {
                        navController.navigate("account_switch")
                    },
                    onNavigateToRelationships = { type ->
                        navController.navigate("relationships/$type")
                    }
                )
            }

            // --- 二级页面 ---
            // Persona主页
            composable(
                route = "persona_profile/{personaId}",
                arguments = listOf(navArgument("personaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val personaId = backStackEntry.arguments?.getString("personaId") ?: ""
                PersonaProfileScreen(
                    onBack = { navController.popBackStack() },
                    onStartChat = {
                        navController.navigate("chat/$personaId")
                    },
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    }
                )
            }

            // 聊天页
            composable(
                route = "chat/{personaId}",
                arguments = listOf(navArgument("personaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val personaId = backStackEntry.arguments?.getString("personaId") ?: ""
                ChatScreen(
                    onBack = { navController.popBackStack() },
                    onProfileClick = {
                        navController.navigate("persona_profile/$personaId")
                    },
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    }
                )
            }

            composable("create_post") {
                CreatePostScreen(
                    onBack = { navController.popBackStack() },
                    onPostCreated = { newPostId ->
                        navController.popBackStack() // 先关掉发帖页
                        navController.navigate("post_detail/$newPostId") // 再跳去详情页
                    }
                )
            }

            //帖子页
            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) {
                PostDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAuthorClick = { authorId ->
                        navController.navigate("persona_profile/$authorId")
                    },
                    onNavigateToForward = { postId ->
                        navController.navigate("forward_select/$postId")
                    }
                )
            }

            // 转发页
            composable(
                route = "forward_select/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) {
                ForwardSelectScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 切换账号页面
            composable("account_switch") {
                AccountSwitchScreen(
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        // 登录成功，回到主页，并清空回退栈
                        navController.navigate("feed") {
                            popUpTo(0) // 清空所有
                        }
                    },
                    onAddAccount = {
                        navController.navigate("login")
                    }
                )
            }

            // 通知页面
            composable("notifications") {
                NotificationScreen(
                    onBack = { navController.popBackStack() },
                    onCommentClick = { postId -> navController.navigate("post_detail/$postId") }
                )
            }

            // 社交关系页面
            composable(
                route = "relationships/{type}", // type: 0=关注, 1=粉丝
                arguments = listOf(navArgument("type") { type = NavType.IntType })
            ) { backStack ->
                val type = backStack.arguments?.getInt("type") ?: 0
                RelationshipScreen(
                    initialTab = type,
                    onBack = { navController.popBackStack() },
                    onPersonaClick = { id -> navController.navigate("persona_profile/$id") }
                )
            }
        }
    }
}

@Composable
fun AuthNavigation(onLoginSuccess: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // 1. 登录页
        composable("login") { backStackEntry ->
            val viewModel: com.example.personai.ui.auth.AuthViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()

            val savedStateHandle = backStackEntry.savedStateHandle
            val registrantInfo = savedStateHandle.get<List<String>>("registrant_info")

            LaunchedEffect(registrantInfo) {
                if (registrantInfo != null && registrantInfo.size == 2) {
                    viewModel.fillCredentials(registrantInfo[0], registrantInfo[1])
                    // 取完后清除，防止旋转屏幕再次填充
                    savedStateHandle.remove<List<String>>("registrant_info")
                }
            }

            LoginScreen(
                onLoginSuccess = onLoginSuccess,
                onGoToRegister = { navController.navigate("register") },
                onNavigateToSwitchAccount = { navController.navigate("account_switch") }
            )
        }

        // 2. 注册页
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { phone, pass ->
                    // 注册成功，回到登录页自动填充 (逻辑同 AuthNavigation)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("registrant_info", listOf(phone, pass))
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 快速登录
        composable("account_switch") {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                AccountSwitchScreen(
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = onLoginSuccess,
                    onAddAccount = {
                        navController.popBackStack(route = "login", inclusive = false)
                    }
                )
            }
        }
    }
}