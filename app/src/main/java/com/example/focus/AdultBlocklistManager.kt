package com.example.focus

import android.content.Context
import android.util.Log
import java.util.Locale

/**
 * On-Device Adult Blocklist & Safe-Word Rules Manager.
 *
 * Stores and manages:
 * 1. Comprehensive built-in adult domains (English, International, Indian).
 * 2. Explicit keywords & phrases in English, Hindi (देवनागरी), and Hinglish.
 * 3. Safe-words (Whitelist) preventing false positives on educational/geographical terms.
 * 4. User-customizable blocklist: Allows users to add or remove domains, keywords, and safe-words.
 * 5. 100% offline & local; zero telemetry or network transmission.
 */
object AdultBlocklistManager {

  private const val TAG = "AdultBlocklistManager"
  private const val PREFS_NAME = "life_tracker_adult_blocklist_prefs"

  private const val KEY_CUSTOM_DOMAINS = "custom_blocked_domains"
  private const val KEY_CUSTOM_KEYWORDS = "custom_blocked_keywords"
  private const val KEY_CUSTOM_SAFE_WORDS = "custom_safe_words"

  // =========================================================================
  // 1. BUILT-IN ADULT DOMAINS (150+ POPULAR ADULT WEBSITES & TUBES)
  // =========================================================================
  val BUILTIN_DOMAINS = hashSetOf(
    // Global Major Tubes & Portals
    "pornhub", "xvideos", "xnxx", "redtube", "youporn", "brazzers",
    "stripchat", "chaturbate", "xhamster", "onlyfans", "rule34", "beeg",
    "spankbang", "hentai", "eporner", "tube8", "tnaflix", "heavy-r",
    "faphouse", "cam4", "camsoda", "bongacams", "livejasmin", "adultfriendfinder",
    "noodlemagazine", "javhd", "javmost", "motherless", "xcafe", "redwap",
    "porn555", "pornone", "hdporno", "bdsmlr", "fetlife", "nudevista",
    "eroticmv", "spankwire", "sublimedir", "youjizz", "pornmd", "hqporner",
    "txxx", "tubepornclassic", "upornia", "voyeurweb", "daftsex", "vporn",
    "nuvid", "sunporno", "porndoe", "slutload", "keezmovies", "gotporn",
    "porn300", "porndig", "pornktube", "pornhits", "xtube", "drdrporn",
    "palimpalim", "fuq", "lobstertube", "pornrabbit", "porngo", "badjojo",
    "dump", "freeones", "twistys", "bangbros", "naughtyamerica", "realitykings",
    "evilangel", "wicked", "digitalplayground", "kink.com", "metart",

    // Indian & South Asian Explicit Portals & Stems
    "desipapa", "kamababa", "antavna", "chudai", "desi49", "fsiblog",
    "hindisexstories", "antavasna", "desikahani", "indiansex", "bhabhisex",
    "savita bhabhi", "savitabhabhi", "kamabishesh", "antarvasna", "desi-porn",
    "desiporn", "indianporn", "bhabhiporn", "malluporn", "tamilsex", "telugusex",
    "kamasutra", "hindisex", "devarbhabhi", "desimms", "viralsex", "kamababa",
    "chudaimms", "bhabhichudai", "masaladesi", "desikahaniya", "desisex",

    // Cams & Live Explicit Services
    "flirt4free", "myfreecams", "streamate", "imlive", "camwhores",
    "webcamromance", "camcaps", "chatrandom", "omegle-adult", "dirtyroulette",

    // Anime / Manga / Hentai Explicit
    "nhentai", "hentaihaven", "hanime", "tsumino", "hitomi.la", "pururin",
    "e-hentai", "gelbooru", "danbooru-nsfw", "sankakucomplex", "doujins"
  )

  val BUILTIN_TLDS = listOf(
    ".xxx", ".porn", ".adult", ".sexy"
  )

  // =========================================================================
  // 2. BUILT-IN EXPLICIT KEYWORDS & PHRASES (ENGLISH, HINDI, HINGLISH)
  // =========================================================================
  val BUILTIN_STANDALONE_WORDS = hashSetOf(
    // English
    "porn", "pornography", "porno", "xxx", "xxnx", "xvideo", "xvideos",
    "nsfw", "hentai", "ecchi", "erotica", "jav", "milf", "blowjob", "creampie",
    "gangbang", "masturbat", "cumshot", "deepthroat", "orgasm", "cunnilingus",
    "fellatio", "shemale", "bukkake", "dildo", "fleshlight",

    // Hindi (Devanagari)
    "पोर्न", "पोर्नोग्राफी", "ब्लूफिल्म", "ब्लू फिल्म", "चुदाई", "संभोग", "कामुक",
    "हस्तमैथुन", "नग्नता", "छिनाल", "योनि", "लिंग चूसना", "मुठ मारना", "गांड चुदाई",
    "नंगापन", "कामाग्नि", "कामुकता", "कामोत्तेजक"
  )

  val BUILTIN_PHRASES = listOf(
    // English
    "sex video", "sexy video", "hardcore sex", "boobs video", "pussy video",
    "nude video", "naked video", "naked girl", "nude girl", "erotic video",
    "adult video", "adult film", "adult movie", "hot sex", "desi sex",
    "bhabhi sex", "full sex", "desi mms", "leaked mms", "call girl sex",
    "camgirl live", "erotica uncensored", "adult tube", "adult film 18+",
    "uncensored 18+", "18+ adult video", "hot nude", "sex clip", "sex scene",
    "sex tape", "sex chat", "watch porn", "free porn", "hd porn", "watch adult",

    // Hindi (Devanagari)
    "सेक्स वीडियो", "नग्न वीडियो", "हॉट सेक्स", "देसी सेक्स", "भाभी सेक्स",
    "चुदाई वीडियो", "संभोग वीडियो", "अडल्ट वीडियो", "गांड सेक्स", "ब्लू पिक्चर",
    "नंगी लड़की", "नंगा वीडियो", "देसी एमएमएस", "चुदाई क्लिप", "संभोग दृश्य",
    "वयस्क चलचित्र", "कामुक वीडियो", "हॉट वीडियो सेक्स",

    // Hinglish
    "chudai video", "chudai sexy", "sambhog video", "blue film", "blue picture",
    "hot bhabhi video", "desi mms leak", "sex movie", "chudai clip", "chudai mms",
    "nangi ladki", "nangi video", "nude video clip", "sexy blue film", "adult movie hindi",
    "desi leaked video", "desi viral mms", "bhabhi chudai", "hot sex video",
    "aunty sex", "devar bhabhi sex", "desi porn video", "mms viral sex"
  )

  // =========================================================================
  // 3. BUILT-IN SAFE WHITELIST (PREVENT FALSE POSITIVES)
  // =========================================================================
  val BUILTIN_SAFE_WORDS = listOf(
    // Geographic names containing 'sex'
    "essex", "sussex", "wessex", "middlesex",

    // Educational, Medical & Biological Terms
    "sex education", "sexual health", "sexual harassment", "sex ratio",
    "gender and sex", "same-sex", "sexist", "secondary", "section",
    "insect", "intersex", "unisex", "bisexual history", "sexual assault awareness",
    "biology sex chromosomes", "sex chromosome", "biology", "genetics",

    // Common non-adult words with overlapping letters
    "middlesex university", "essex county", "sussex county", "sextant",
    "sextet", "prosecute", "intersection"
  )

  // =========================================================================
  // 4. USER CUSTOM MANAGEMENT (ADD / REMOVE / RESET)
  // =========================================================================

  fun getCustomBlockedDomains(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()
  }

  fun addCustomDomain(context: Context, domain: String): Boolean {
    val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
    if (clean.isBlank()) return false
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = getCustomBlockedDomains(context).toMutableSet()
    set.add(clean)
    prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, set).apply()
    Log.d(TAG, "Added custom blocked domain: $clean")
    return true
  }

  fun removeCustomDomain(context: Context, domain: String, pinVerified: Boolean = false): Boolean {
    if (!pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot remove custom domain: PIN or 24-hr unlock required")
        return false
      }
    }
    val clean = domain.trim().lowercase()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = getCustomBlockedDomains(context).toMutableSet()
    set.remove(clean)
    prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, set).apply()
    Log.d(TAG, "Removed custom blocked domain: $clean")
    return true
  }

  fun getCustomBlockedKeywords(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_CUSTOM_KEYWORDS, emptySet()) ?: emptySet()
  }

  fun addCustomKeyword(context: Context, keyword: String): Boolean {
    val clean = keyword.trim().lowercase()
    if (clean.isBlank() || clean.length < 2) return false
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = getCustomBlockedKeywords(context).toMutableSet()
    set.add(clean)
    prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, set).apply()
    Log.d(TAG, "Added custom blocked keyword: $clean")
    return true
  }

  fun removeCustomKeyword(context: Context, keyword: String, pinVerified: Boolean = false): Boolean {
    if (!pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot remove custom keyword: PIN or 24-hr unlock required")
        return false
      }
    }
    val clean = keyword.trim().lowercase()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = getCustomBlockedKeywords(context).toMutableSet()
    set.remove(clean)
    prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, set).apply()
    Log.d(TAG, "Removed custom blocked keyword: $clean")
    return true
  }

  fun getCustomSafeWords(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_CUSTOM_SAFE_WORDS, emptySet()) ?: emptySet()
  }

  fun addCustomSafeWord(context: Context, word: String, pinVerified: Boolean = false): Boolean {
    if (!pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot add custom safe word (weakens protection): PIN or 24-hr unlock required")
        return false
      }
    }
    val clean = word.trim().lowercase()
    if (clean.isBlank() || clean.length < 2) return false
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = getCustomSafeWords(context).toMutableSet()
    set.add(clean)
    prefs.edit().putStringSet(KEY_CUSTOM_SAFE_WORDS, set).apply()
    Log.d(TAG, "Added custom safe word: $clean")
    return true
  }

  fun removeCustomSafeWord(context: Context, word: String) {
    val clean = word.trim().lowercase()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = getCustomSafeWords(context).toMutableSet()
    set.remove(clean)
    prefs.edit().putStringSet(KEY_CUSTOM_SAFE_WORDS, set).apply()
    Log.d(TAG, "Removed custom safe word: $clean")
  }

  // =========================================================================
  // 5. QUERY & VALIDATION APIS
  // =========================================================================

  /**
   * Fast domain check used by Local DNS VPN Service.
   * Checks if [domain] matches any built-in or custom adult domain.
   */
  fun isDomainBlocked(context: Context, domain: String): Boolean {
    val clean = domain.trim().lowercase().removePrefix("www.")
    if (clean.isBlank()) return false

    // Check custom safe words
    val customSafe = getCustomSafeWords(context)
    for (safe in customSafe) {
      if (clean.contains(safe)) return false
    }

    // Check custom domains first
    val customDomains = getCustomBlockedDomains(context)
    for (d in customDomains) {
      if (clean.contains(d)) return true
    }

    // Check built-in adult TLDs
    for (tld in BUILTIN_TLDS) {
      if (clean.endsWith(tld)) return true
    }

    // Check built-in domain stems
    for (stem in BUILTIN_DOMAINS) {
      if (clean.contains(stem)) return true
    }

    return false
  }

  /**
   * Evaluates text and returns match result or null if clean.
   */
  fun checkContent(context: Context, rawText: String, packageName: String): AdultContentDetector.DetectionResult? {
    if (rawText.isBlank() || rawText.length < 3) return null

    val normalized = normalizeText(rawText)

    // 1. Safe Whitelist Check (Built-in + Custom)
    for (safe in BUILTIN_SAFE_WORDS) {
      if (normalized.contains(safe)) return null
    }
    val customSafe = getCustomSafeWords(context)
    for (safe in customSafe) {
      if (normalized.contains(safe)) return null
    }

    // 2. Custom Blocked Domains & Keywords
    val customDomains = getCustomBlockedDomains(context)
    for (domain in customDomains) {
      if (normalized.contains(domain)) {
        Log.i(TAG, "Custom domain match: '$domain' in '$rawText'")
        return AdultContentDetector.DetectionResult(
          isAdult = true,
          matchedTerm = domain,
          category = "User Blocked Domain",
          sourcePackage = packageName
        )
      }
    }

    val customKeywords = getCustomBlockedKeywords(context)
    for (kw in customKeywords) {
      if (normalized.contains(kw)) {
        Log.i(TAG, "Custom keyword match: '$kw' in '$rawText'")
        return AdultContentDetector.DetectionResult(
          isAdult = true,
          matchedTerm = kw,
          category = "User Blocked Keyword",
          sourcePackage = packageName
        )
      }
    }

    // 3. Built-in Adult Domains
    for (domain in BUILTIN_DOMAINS) {
      if (normalized.contains(domain)) {
        Log.i(TAG, "Built-in domain stem match: '$domain' in '$rawText'")
        return AdultContentDetector.DetectionResult(
          isAdult = true,
          matchedTerm = domain,
          category = "Adult Domain",
          sourcePackage = packageName
        )
      }
    }

    // Built-in TLDs
    for (tld in BUILTIN_TLDS) {
      if (normalized.contains(tld)) {
        Log.i(TAG, "Built-in TLD match: '$tld' in '$rawText'")
        return AdultContentDetector.DetectionResult(
          isAdult = true,
          matchedTerm = tld,
          category = "Adult Domain TLD",
          sourcePackage = packageName
        )
      }
    }

    // 4. Built-in Explicit Phrases
    for (phrase in BUILTIN_PHRASES) {
      if (normalized.contains(phrase)) {
        Log.i(TAG, "Built-in explicit phrase match: '$phrase' in '$rawText'")
        return AdultContentDetector.DetectionResult(
          isAdult = true,
          matchedTerm = phrase,
          category = "Explicit Phrase",
          sourcePackage = packageName
        )
      }
    }

    // 5. Standalone Words with token boundaries
    val tokens = normalized.split(Regex("[\\s.,:;/?!@#%^&*()\\[\\]\\-_+=|~<>\"']+"))
    for (token in tokens) {
      if (token.length >= 3 && BUILTIN_STANDALONE_WORDS.contains(token)) {
        Log.i(TAG, "Built-in explicit word match: '$token' in '$rawText'")
        return AdultContentDetector.DetectionResult(
          isAdult = true,
          matchedTerm = token,
          category = "Explicit Keyword",
          sourcePackage = packageName
        )
      }
    }

    return null
  }

  private fun normalizeText(text: String): String {
    var s = text.lowercase(Locale.ROOT)
    // Leetspeak normalization
    s = s.replace('0', 'o')
      .replace('@', 'a')
      .replace('$', 's')
      .replace('1', 'i')
      .replace('!', 'i')
      .replace('3', 'e')
      .replace('*', ' ')
    return s
  }
}
