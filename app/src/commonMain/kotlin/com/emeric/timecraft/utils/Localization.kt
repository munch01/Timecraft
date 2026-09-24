package com.emeric.timecraft.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.emeric.timecraft.getSettingsStorage

enum class AppLanguage(val code: String, val displayName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: FRENCH

        fun fromDisplayName(name: String): AppLanguage =
            entries.firstOrNull { it.displayName == name } ?: FRENCH
    }
}

object LanguageManager {
    private val storage by lazy { getSettingsStorage() }
    var currentLanguage by mutableStateOf(loadLanguage())
        private set

    private fun loadLanguage(): AppLanguage {
        val code = try {
            getSettingsStorage().getString("app_language", "fr")
        } catch (e: Exception) {
            "fr"
        }
        return AppLanguage.fromCode(code)
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        try {
            getSettingsStorage().setString("app_language", language.code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class AppStrings(
    // Common / Navigation
    val calendar: String,
    val trips: String,
    val report: String,
    val settings: String,
    val quit: String,
    val buyCoffee: String,
    val cancel: String,
    val save: String,
    val delete: String,
    val back: String,
    val confirm: String,
    val close: String,
    val email: String,
    val password: String,

    // Auth Screen
    val login: String,
    val signup: String,
    val createAccount: String,
    val noAccount: String,
    val alreadyAccount: String,

    // Settings Screen
    val profileAndAccount: String,
    val preferences: String,
    val appLanguage: String,
    val selectLanguage: String,
    val biometricsOnStartup: String,
    val biometricsEnabled: String,
    val biometricsDisabled: String,
    val defaultSchedules: String,
    val aboutApp: String,
    val changePassword: String,
    val logout: String,
    val version: String,

    // Day & Work Types
    val dayWorked: String,
    val dayRtt: String,
    val dayPaidLeave: String,
    val dayFamilyAbsence: String,
    val dayUnpaidLeave: String,
    val client: String,
    val addClient: String,
    val expenses: String,
    val addExpense: String,
    val notes: String,

    // Reports Screen
    val monthlyReport: String,
    val yearlyReport: String,
    val monthlySummary: String,
    val yearlySummary: String,
    val workedDays: String,
    val rttDays: String,
    val paidLeaveDays: String,
    val familyEventDays: String,
    val unpaidDays: String,
    val gpsDistance: String,
    val totalExpenses: String,
    val weeklyAverage: String,
    val monthlyBase: String,
    val yearlyWeeklyAvg: String,
    val exportPdf: String,
    val clientBreakdown: String,
    val monthByMonth: String,

    // Map Screen
    val gpsTracking: String,
    val startTrip: String,
    val stopTrip: String,
    val distance: String
)

val StringsFr = AppStrings(
    calendar = "Calendrier",
    trips = "Trajets",
    report = "Rapport",
    settings = "Paramètres",
    quit = "Quitter",
    buyCoffee = "Offrir un café",
    cancel = "Annuler",
    save = "Enregistrer",
    delete = "Supprimer",
    back = "Retour",
    confirm = "Confirmer",
    close = "Fermer",
    email = "Email",
    password = "Mot de passe",

    login = "Se connecter",
    signup = "S'inscrire",
    createAccount = "Créer un compte",
    noAccount = "Pas de compte ? S'inscrire",
    alreadyAccount = "Déjà un compte ? Se connecter",

    profileAndAccount = "Profil & Compte",
    preferences = "Préférences",
    appLanguage = "Langue de l'application",
    selectLanguage = "Choisir la langue",
    biometricsOnStartup = "Biométrie au démarrage",
    biometricsEnabled = "Activé au démarrage",
    biometricsDisabled = "Désactivé",
    defaultSchedules = "Horaires par défaut",
    aboutApp = "À propos de TimeCraft",
    changePassword = "Changer le mot de passe",
    logout = "Se déconnecter",
    version = "Version",

    dayWorked = "Travaillé",
    dayRtt = "RTT",
    dayPaidLeave = "Congés payés",
    dayFamilyAbsence = "Evénement familial",
    dayUnpaidLeave = "Congé sans solde",
    client = "Client",
    addClient = "Ajouter un client",
    expenses = "Frais engagés",
    addExpense = "Ajouter un frais",
    notes = "Notes / Remarques",

    monthlyReport = "Rapport Mensuel",
    yearlyReport = "Rapport Annuel",
    monthlySummary = "Synthèse Mensuelle",
    yearlySummary = "Bilan Annuel",
    workedDays = "Jours travaillés",
    rttDays = "RTT pris",
    paidLeaveDays = "Congés payés",
    familyEventDays = "Evénement familial",
    unpaidDays = "Congé sans solde",
    gpsDistance = "Distance GPS parcourue",
    totalExpenses = "Total des frais",
    weeklyAverage = "Moyenne Hebdomadaire",
    monthlyBase = "Base mensuelle",
    yearlyWeeklyAvg = "Moyenne hebdo sur l'année",
    exportPdf = "Exporter le Rapport en PDF",
    clientBreakdown = "Répartition par Client",
    monthByMonth = "Détail Mois par Mois",

    gpsTracking = "Suivi GPS",
    startTrip = "Démarrer le trajet",
    stopTrip = "Arrêter le trajet",
    distance = "Distance"
)

val StringsEn = AppStrings(
    calendar = "Calendar",
    trips = "Trips",
    report = "Report",
    settings = "Settings",
    quit = "Quit",
    buyCoffee = "Buy a coffee",
    cancel = "Cancel",
    save = "Save",
    delete = "Delete",
    back = "Back",
    confirm = "Confirm",
    close = "Close",
    email = "Email",
    password = "Password",

    login = "Log in",
    signup = "Sign up",
    createAccount = "Create an account",
    noAccount = "No account? Sign up",
    alreadyAccount = "Already have an account? Log in",

    profileAndAccount = "Profile & Account",
    preferences = "Preferences",
    appLanguage = "App Language",
    selectLanguage = "Select Language",
    biometricsOnStartup = "Biometrics on startup",
    biometricsEnabled = "Enabled on startup",
    biometricsDisabled = "Disabled",
    defaultSchedules = "Default Schedules",
    aboutApp = "About TimeCraft",
    changePassword = "Change password",
    logout = "Log out",
    version = "Version",

    dayWorked = "Worked",
    dayRtt = "Rest Day (RTT)",
    dayPaidLeave = "Paid Leave",
    dayFamilyAbsence = "Family Event",
    dayUnpaidLeave = "Unpaid Leave",
    client = "Client",
    addClient = "Add client",
    expenses = "Incurred Expenses",
    addExpense = "Add expense",
    notes = "Notes / Comments",

    monthlyReport = "Monthly Report",
    yearlyReport = "Yearly Report",
    monthlySummary = "Monthly Summary",
    yearlySummary = "Yearly Summary",
    workedDays = "Worked days",
    rttDays = "Rest days taken",
    paidLeaveDays = "Paid leave",
    familyEventDays = "Family event",
    unpaidDays = "Unpaid leave",
    gpsDistance = "GPS Distance traveled",
    totalExpenses = "Total expenses",
    weeklyAverage = "Weekly Average",
    monthlyBase = "Monthly base",
    yearlyWeeklyAvg = "Yearly weekly average",
    exportPdf = "Export Report as PDF",
    clientBreakdown = "Client Breakdown",
    monthByMonth = "Month by Month Details",

    gpsTracking = "GPS Tracking",
    startTrip = "Start trip",
    stopTrip = "Stop trip",
    distance = "Distance"
)

val StringsEs = AppStrings(
    calendar = "Calendario",
    trips = "Trayectos",
    report = "Informe",
    settings = "Configuración",
    quit = "Salir",
    buyCoffee = "Invitar a un café",
    cancel = "Cancelar",
    save = "Guardar",
    delete = "Eliminar",
    back = "Volver",
    confirm = "Confirmar",
    close = "Cerrar",
    email = "Correo electrónico",
    password = "Contraseña",

    login = "Iniciar sesión",
    signup = "Registrarse",
    createAccount = "Crear cuenta",
    noAccount = "¿No tienes cuenta? Regístrate",
    alreadyAccount = "¿Ya tienes cuenta? Inicia sesión",

    profileAndAccount = "Perfil y Cuenta",
    preferences = "Preferencias",
    appLanguage = "Idioma de la aplicación",
    selectLanguage = "Elegir idioma",
    biometricsOnStartup = "Biometría al iniciar",
    biometricsEnabled = "Activado al iniciar",
    biometricsDisabled = "Desactivado",
    defaultSchedules = "Horarios por defecto",
    aboutApp = "Acerca de TimeCraft",
    changePassword = "Cambiar contraseña",
    logout = "Cerrar sesión",
    version = "Versión",

    dayWorked = "Trabajado",
    dayRtt = "Día de descanso (RTT)",
    dayPaidLeave = "Vacaciones pagadas",
    dayFamilyAbsence = "Evento familiar",
    dayUnpaidLeave = "Permiso no retribuido",
    client = "Cliente",
    addClient = "Añadir cliente",
    expenses = "Gastos realizados",
    addExpense = "Añadir gasto",
    notes = "Notas / Comentarios",

    monthlyReport = "Informe Mensual",
    yearlyReport = "Informe Anual",
    monthlySummary = "Resumen Mensual",
    yearlySummary = "Resumen Anual",
    workedDays = "Días trabajados",
    rttDays = "Días RTT tomados",
    paidLeaveDays = "Vacaciones pagadas",
    familyEventDays = "Evento familiar",
    unpaidDays = "Permiso no retribuido",
    gpsDistance = "Distancia GPS recorrida",
    totalExpenses = "Total de gastos",
    weeklyAverage = "Promedio Semanal",
    monthlyBase = "Base mensual",
    yearlyWeeklyAvg = "Promedio semanal anual",
    exportPdf = "Exportar Informe a PDF",
    clientBreakdown = "Desglose por Cliente",
    monthByMonth = "Detalle Mes a Mes",

    gpsTracking = "Rastreo GPS",
    startTrip = "Iniciar trayecto",
    stopTrip = "Detener trayecto",
    distance = "Distancia"
)

fun getAppStrings(language: AppLanguage = LanguageManager.currentLanguage): AppStrings = when (language) {
    AppLanguage.FRENCH -> StringsFr
    AppLanguage.ENGLISH -> StringsEn
    AppLanguage.SPANISH -> StringsEs
}

val LocalAppStrings = staticCompositionLocalOf { StringsFr }
