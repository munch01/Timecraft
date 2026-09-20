package com.example.timecraft.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://zaykharbtuxifzlrecob.supabase.co",
    supabaseKey = "sb_publishable_KOWGwI9CKmdWCzPGsxDd7w_yk9eBCiE"
) {
    install(Auth)
    install(Postgrest)
}
