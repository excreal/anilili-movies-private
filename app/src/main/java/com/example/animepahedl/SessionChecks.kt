package com.example.animepahedl

import org.jsoup.Jsoup

/** Mirrors src/http.rs: a normal Cloudflare jsd script is not a CAPTCHA. */
internal object SessionChecks {
    private val titles = listOf("just a moment", "attention required", "verify you are human", "security verification")
    private const val TOKENS = "[name=cf-turnstile-response], [name=g-recaptcha-response], [name=h-captcha-response]"
    private const val WIDGETS = ".cf-turnstile, .g-recaptcha, .h-captcha, #aws-waf-captcha-container, #challenge-form, iframe[src*=challenges.cloudflare.com], iframe[src*=hcaptcha.com], iframe[src*=google.com/recaptcha/]"

    fun isChallenge(body: String, mitigated: String? = null): Boolean {
        if (mitigated.equals("challenge", true)) return true
        val document = Jsoup.parse(body)
        if (titles.any { document.title().contains(it, true) }) return true
        val solved = document.select(TOKENS).any { it.`val`().isNotBlank() || it.text().isNotBlank() }
        return !solved && document.select(WIDGETS).any { element ->
            (listOf(element) + element.parents()).none { ancestor ->
                val style = ancestor.attr("style").replace(Regex("\\s"), "").lowercase()
                ancestor.hasAttr("hidden") || "display:none" in style || "visibility:hidden" in style
            }
        }
    }

    // The same readiness rules as the Rust page_ready.js; no challenge-solving code.
    val browserStateScript = """
        (() => {
          const state = (ready, reason) => JSON.stringify({ready, reason, url:location.href});
          if (location.protocol !== 'https:') return state(false, 'Waiting for HTTPS');
          if (/just a moment|attention required|verify you are human|security verification/i.test(document.title))
            return state(false, 'Waiting for verification and destination navigation');
          const visible = e => e.getClientRects().length > 0 && getComputedStyle(e).visibility !== 'hidden';
          if ([...document.querySelectorAll('#challenge-running,#challenge-stage,#cf-challenge-running,#challenge-form')].some(visible))
            return state(false, 'Waiting for the challenge to finish');
          const widgets = document.querySelectorAll('.cf-turnstile,.g-recaptcha,.h-captcha,#aws-waf-captcha-container,iframe[src*="challenges.cloudflare.com"],iframe[src*="hcaptcha.com"],iframe[src*="google.com/recaptcha/"]');
          const solved = [...document.querySelectorAll('[name="cf-turnstile-response"],[name="g-recaptcha-response"],[name="h-captcha-response"]')].some(e => e.value.trim());
          if ([...widgets].some(visible) && !solved) return state(false, 'Please complete the CAPTCHA manually');
          if (document.readyState !== 'complete') return state(false, 'Loading destination');
          return state(true, 'Destination loaded without a detected challenge');
        })()
    """.trimIndent()
}
