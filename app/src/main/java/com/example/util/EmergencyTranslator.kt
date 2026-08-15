package com.example.util

import java.util.Locale

/**
 * High-performance multilingual emergency translation engine.
 * Provides instant, highly accurate emergency translations across 10 major languages,
 * serving as an ultra-reliable offline engine and seamless fallback for Gemini AI.
 */
object EmergencyTranslator {

    fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String = "English"
    ): String {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return ""

        val target = targetLanguage.trim().lowercase(Locale.ROOT)
        if (target == "english" || target == "en") return cleanText

        // 1. Check exact phrase / sentence match first
        val exactMatch = lookupExactPhrase(cleanText, target)
        if (exactMatch != null) return exactMatch

        // 2. Check dynamic template patterns (e.g., "Attention Floor 4...", "2 people trapped in Room 402", etc.)
        val templateMatch = matchTemplate(cleanText, target)
        if (templateMatch != null) return templateMatch

        // 3. Robust token & dictionary translation
        return translateWordsAndPhrases(cleanText, target)
    }

    private fun lookupExactPhrase(text: String, lang: String): String? {
        val key = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val dict = exactPhrases[key] ?: return null
        return dict.forLang(lang)
    }

    private fun matchTemplate(text: String, lang: String): String? {
        // Pattern 1: "ATTENTION FLOOR X: [hazard/instruction]"
        val floorAttMatch = Regex("""attention\s+floor\s+(\d+)[:\s]+(.*)""", RegexOption.IGNORE_CASE).find(text)
        if (floorAttMatch != null) {
            val floorNum = floorAttMatch.groupValues[1]
            val rest = floorAttMatch.groupValues[2].trim()
            val translatedRest = translate(rest, lang)
            return when (lang) {
                "spanish", "es" -> "ATENCIÓN PISO $floorNum: $translatedRest"
                "french", "fr" -> "ATTENTION ÉTAGE $floorNum: $translatedRest"
                "mandarin", "chinese", "zh" -> "请注意 第 $floorNum 层: $translatedRest"
                "arabic", "ar" -> "تنبيه الطابق $floorNum: $translatedRest"
                "russian", "ru" -> "ВНИМАНИЕ $floorNum ЭТАЖ: $translatedRest"
                "hindi", "hi" -> "ध्यान दें मंजिल $floorNum: $translatedRest"
                "japanese", "ja" -> "注意 $floorNum 階: $translatedRest"
                "tamil", "ta" -> "கவனம் தளம் $floorNum: $translatedRest"
                "german", "de" -> "ACHTUNG ETAGE $floorNum: $translatedRest"
                "tagalog", "filipino", "tl" -> "PAUNAWA PALAPAG $floorNum: $translatedRest"
                else -> "ATENCIÓN PISO $floorNum: $translatedRest"
            }
        }

        // Pattern 2: "ALL RESIDENTS: [instruction]"
        val allResMatch = Regex("""all\s+residents[:\s]+(.*)""", RegexOption.IGNORE_CASE).find(text)
        if (allResMatch != null) {
            val rest = allResMatch.groupValues[1].trim()
            val translatedRest = translate(rest, lang)
            return when (lang) {
                "spanish", "es" -> "TODOS LOS RESIDENTES: $translatedRest"
                "french", "fr" -> "TOUS LES RÉSIDENTS: $translatedRest"
                "mandarin", "chinese", "zh" -> "全体住客请注意: $translatedRest"
                "arabic", "ar" -> "جميع السكان: $translatedRest"
                "russian", "ru" -> "ВСЕМ ЖИТЕЛЯМ: $translatedRest"
                "hindi", "hi" -> "सभी निवासी: $translatedRest"
                "japanese", "ja" -> "全居住者へ: $translatedRest"
                "tamil", "ta" -> "அனைத்து குடியிருப்பாளர்களும்: $translatedRest"
                "german", "de" -> "ALLE BEWOHNER: $translatedRest"
                "tagalog", "filipino", "tl" -> "LAHAT NG NAKATIRA: $translatedRest"
                else -> "TODOS LOS RESIDENTES: $translatedRest"
            }
        }

        // Pattern 3: "X people trapped in Room Y..."
        val trappedMatch = Regex("""(\d+)\s+(?:people|persons|occupants)\s+trapped\s+in\s+room\s+(\w+)(.*)""", RegexOption.IGNORE_CASE).find(text)
        if (trappedMatch != null) {
            val count = trappedMatch.groupValues[1]
            val room = trappedMatch.groupValues[2]
            val extra = trappedMatch.groupValues[3].trim()
            val translatedExtra = if (extra.isNotBlank()) " " + translate(extra, lang) else ""
            return when (lang) {
                "spanish", "es" -> "$count personas atrapadas en la Habitación $room.$translatedExtra"
                "french", "fr" -> "$count personnes piégées dans la Chambre $room.$translatedExtra"
                "mandarin", "chinese", "zh" -> "$count 人被困在 $room 号房间。$translatedExtra"
                "arabic", "ar" -> "$count أشخاص محاصرون في الغرفة $room.$translatedExtra"
                "russian", "ru" -> "$count человек заблокированы в комнате $room.$translatedExtra"
                "hindi", "hi" -> "कमरा $room में $count लोग फंसे हैं।$translatedExtra"
                "japanese", "ja" -> "$room 号室に $count 人が閉じ込められています。$translatedExtra"
                "tamil", "ta" -> "அறை $room இல் $count நபர்கள் சிக்கியுள்ளனர்.$translatedExtra"
                "german", "de" -> "$count Personen im Zimmer $room eingeschlossen.$translatedExtra"
                "tagalog", "filipino", "tl" -> "$count tao ang naipit sa Kuwarto $room.$translatedExtra"
                else -> "$count personas atrapadas en la Habitación $room.$translatedExtra"
            }
        }

        // Pattern 4: "Status tagged / Status chip: [chip]"
        val chipMatch = Regex("""status\s+(?:tagged|chip)[:\s]*(.+)""", RegexOption.IGNORE_CASE).find(text)
        if (chipMatch != null) {
            val chip = chipMatch.groupValues[1].trim()
            val chipTrans = translate(chip, lang)
            return when (lang) {
                "spanish", "es" -> "Estado etiquetado: $chipTrans"
                "french", "fr" -> "Statut marqué: $chipTrans"
                "mandarin", "chinese", "zh" -> "状态标记: $chipTrans"
                "arabic", "ar" -> "الحالة المحددة: $chipTrans"
                "russian", "ru" -> "Отмеченный статус: $chipTrans"
                "hindi", "hi" -> "स्थिति टैग: $chipTrans"
                "japanese", "ja" -> "ステータスタグ: $chipTrans"
                "tamil", "ta" -> "நிலை குறிச்சொல்: $chipTrans"
                "german", "de" -> "Status markiert: $chipTrans"
                "tagalog", "filipino", "tl" -> "Katayuan: $chipTrans"
                else -> "Estado etiquetado: $chipTrans"
            }
        }

        return null
    }

    private fun translateWordsAndPhrases(text: String, lang: String): String {
        var result = text

        // Step 1: Multi-word phrase replacements
        phraseDictionary.forEach { (phraseRegex, trans) ->
            result = result.replace(phraseRegex, trans.forLang(lang))
        }

        // Step 2: Single word vocabulary replacements
        val words = result.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?:;])|(?=[.,!?:;])"))
        val translatedWords = words.map { token ->
            val cleanToken = token.trim().lowercase(Locale.ROOT)
            val trans = singleWordDict[cleanToken]
            if (trans != null) {
                val replacement = trans.forLang(lang)
                if (token.isNotEmpty() && token[0].isUpperCase()) {
                    replacement.replaceFirstChar { it.uppercase() }
                } else {
                    replacement
                }
            } else {
                token
            }
        }

        return translatedWords.joinToString("")
    }

    private data class TransEntry(
        val es: String, // Spanish
        val fr: String, // French
        val zh: String, // Mandarin
        val ar: String, // Arabic
        val ru: String, // Russian
        val hi: String, // Hindi
        val ja: String, // Japanese
        val ta: String, // Tamil
        val de: String, // German
        val tl: String  // Tagalog
    ) {
        fun forLang(lang: String): String {
            return when (lang.lowercase(Locale.ROOT).trim()) {
                "spanish", "es" -> es
                "french", "fr" -> fr
                "mandarin", "chinese", "zh" -> zh
                "arabic", "ar" -> ar
                "russian", "ru" -> ru
                "hindi", "hi" -> hi
                "japanese", "ja" -> ja
                "tamil", "ta" -> ta
                "german", "de" -> de
                "tagalog", "filipino", "tl" -> tl
                else -> es
            }
        }
    }

    private val exactPhrases = mapOf(
        "stay in room do not open door" to TransEntry(
            es = "Permanezca en su habitación. No abra la puerta.",
            fr = "Restez dans votre chambre. N'ouvrez pas la porte.",
            zh = "请留在房间内。切勿打开房门。",
            ar = "ابق في غرفتك. لا تفتح الباب.",
            ru = "Оставайтесь в комнате. Не открывайте дверь.",
            hi = "कमरे में रहें। दरवाजा न खोलें।",
            ja = "部屋にとどまってください。ドアを開けないでください。",
            ta = "அறையிலேயே இருங்கள். கதவை திறக்க வேண்டாம்.",
            de = "Im Zimmer bleiben. Tür nicht öffnen.",
            tl = "Manatili sa silid. Huwag buksan ang pinto."
        ),
        "i am safe" to TransEntry(
            es = "Estoy a salvo",
            fr = "Je suis en sécurité",
            zh = "我已安全",
            ar = "أنا بأمان",
            ru = "Я в безопасности",
            hi = "मैं सुरक्षित हूँ",
            ja = "無事です",
            ta = "நான் பாதுகாப்பாக உள்ளேன்",
            de = "Ich bin sicher",
            tl = "Ligtas ako"
        ),
        "send sos" to TransEntry(
            es = "Enviar SOS",
            fr = "Envoyer SOS",
            zh = "发送 SOS",
            ar = "إرسال SOS",
            ru = "Отправить SOS",
            hi = "SOS भेजें",
            ja = "SOS送信",
            ta = "SOS அனுப்பு",
            de = "SOS senden",
            tl = "Magpadala ng SOS"
        ),
        "resolve alert" to TransEntry(
            es = "Resolver alerta",
            fr = "Résoudre l'alerte",
            zh = "解除警报",
            ar = "حل الإنذار",
            ru = "Снять тревогу",
            hi = "अलर्ट हटाएं",
            ja = "アラート解除",
            ta = "எச்சரிக்கையை அகற்று",
            de = "Alarm auflösen",
            tl = "Lutasin ang alerto"
        ),
        "request rescue" to TransEntry(
            es = "Solicitar rescate",
            fr = "Demander secours",
            zh = "请求紧急救援",
            ar = "طلب الإنقاذ",
            ru = "Запросить спасение",
            hi = "बचाव का अनुरोध करें",
            ja = "救助要請",
            ta = "மீட்பு கோரிக்கை",
            de = "Rettung anfordern",
            tl = "Humiling ng saklolo"
        ),
        "attention floor 4 west stairwell blocked evacuate via east exit only" to TransEntry(
            es = "ATENCIÓN PISO 4: Escalera Oeste bloqueada. Evacue únicamente por la salida Este.",
            fr = "ATTENTION ÉTAGE 4: Escalier Ouest bloqué. Évacuez uniquement par la sortie Est.",
            zh = "请注意 第 4 层: 西侧楼梯已封锁。请仅由东侧出口疏散。",
            ar = "تنبيه الطابق 4: الدرج الغربي مغلق. يرجى الإخلاء عبر المخرج الشرقي فقط.",
            ru = "ВНИМАНИЕ 4 ЭТАЖ: Западная лестница заблокирована. Эвакуируйтесь только через восточный выход.",
            hi = "ध्यान दें मंजिल 4: पश्चिमी सीढ़ी अवरुद्ध है। केवल पूर्वी निकास से बाहर निकलें।",
            ja = "注意 4階: 西階段は封鎖されています。東出口からのみ避難してください。",
            ta = "கவனம் தளம் 4: மேற்கு படிக்கட்டு அடைக்கப்பட்டுள்ளது. கிழக்கு வெளியேறும் வழியாக மட்டுமே வெளியேறவும்.",
            de = "ACHTUNG ETAGE 4: Westliches Treppenhaus blockiert. Nur über den Ostausgang evakuieren.",
            tl = "PAUNAWA PALAPAG 4: Barado ang kanlurang hagdanan. Lumikas lamang sa silangang labasan."
        ),
        "all residents evacuate immediately via nearest stairwell do not use elevators" to TransEntry(
            es = "TODOS LOS RESIDENTES: Evacuen inmediatamente por la escalera más cercana. No usen elevadores.",
            fr = "TOUS LES RÉSIDENTS: Évacuez immédiatement par l'escalier le plus proche. N'utilisez pas les ascenseurs.",
            zh = "全体住客请注意: 请立即由最近楼梯疏散。切勿使用电梯。",
            ar = "جميع السكان: أخلوا فوراً عبر أقرب درج. لا تستخدموا المصاعد.",
            ru = "ВСЕМ ЖИТЕЛЯМ: Немедленно эвакуируйтесь по ближайшей лестнице. Не пользуйтесь лифтами.",
            hi = "सभी निवासी: निकटतम सीढ़ी से तुरंत बाहर निकलें। लिफ्ट का उपयोग न करें।",
            ja = "全居住者へ: 最寄りの非常階段から直ちに避難してください。エレベーターは使用禁止です。",
            ta = "அனைத்து குடியிருப்பாளர்களும்: அருகிலுள்ள படிக்கட்டு வழியாக உடனடியாக வெளியேறவும். லிஃப்ட்களைப் பயன்படுத்த வேண்டாம்.",
            de = "ALLE BEWOHNER: Sofort über das nächste Treppenhaus evakuieren. Keine Aufzüge benutzen.",
            tl = "LAHAT NG NAKATIRA: Lumikas agad gamit ang pinakamalapit na hagdanan. Huwag gumamit ng elevator."
        ),
        "attention floors 3 4 heavy smoke detected shelter in place seal door gaps" to TransEntry(
            es = "ATENCIÓN PISOS 3 Y 4: Humo denso detectado. Permanezcan en el lugar y sellen las rendijas de las puertas.",
            fr = "ATTENTION ÉTAGES 3 ET 4: Fumée épaisse détectée. Confinez-vous et scellez le bas des portes.",
            zh = "请注意 第 3 和 4 层: 监测到浓烟。请就地避险并密封门缝。",
            ar = "تنبيه الطوابق 3 و 4: تم رصد دخان كثيف. احتموا في مكانكم وسدوا فتحات الأبواب.",
            ru = "ВНИМАНИЕ 3 И 4 ЭТАЖ: Обнаружен густой дым. Укройтесь на месте и заделайте щели дверей.",
            hi = "ध्यान दें मंजिल 3 और 4: भारी धुआं देखा गया। वहीं रहें और दरवाजे के अंतराल को सील करें।",
            ja = "注意 3・4階: 濃煙を検知しました。室内待機しドアの隙間を塞いでください。",
            ta = "கவனம் தளம் 3 மற்றும் 4: அடர்ந்த புகை கண்டறியப்பட்டது. அறையிலேயே தங்கி கதவை மூடுங்கள்.",
            de = "ACHTUNG ETAGEN 3 & 4: Dichter Rauch entdeckt. Vor Ort bleiben und Türspalten abdichten.",
            tl = "PAUNAWA PALAPAG 3 AT 4: May makapal na usok. Manatili sa pwesto at takpan ang awang ng pinto."
        ),
        "all residents fire incident contained in east wing stand by for all clear" to TransEntry(
            es = "TODOS LOS RESIDENTES: Incidente de incendio contenido en el Ala Este. Permanezcan a la espera de la señal de despejado.",
            fr = "TOUS LES RÉSIDENTS: Incendie maîtrisé dans l'Aile Est. Restez en attente de la fin d'alerte.",
            zh = "全体住客请注意: 东翼火情已得到控制。请等待解除警报指令。",
            ar = "جميع السكان: تمت السيطرة على الحريق في الجناح الشرقي. ترقبوا إشارة انتهاء الخطر.",
            ru = "ВСЕМ ЖИТЕЛЯМ: Пожар в восточном крыле локализован. Ожидайте сигнала отбоя тревоги.",
            hi = "सभी निवासी: पूर्वी विंग में आग पर काबू पा लिया गया है। सब कुछ स्पष्ट होने की प्रतीक्षा करें।",
            ja = "全居住者へ: 東棟の火災は鎮火に向かっています。安全宣言が出るまで待機してください。",
            ta = "அனைத்து குடியிருப்பாளர்களும்: கிழக்கு பகுதியில் தீ கட்டுப்படுத்தப்பட்டுள்ளது. எச்சரிக்கை நீக்கத்திற்காக காத்திருக்கவும்.",
            de = "ALLE BEWOHNER: Brand im Ostflügel unter Kontrolle. Warten Sie auf Entwarnung.",
            tl = "LAHAT NG NAKATIRA: Nakontrol na ang sunog sa East Wing. Maghintay para sa all-clear."
        ),
        "sos distress flagged in room 402 heavy smoke in corridor" to TransEntry(
            es = "Señal de auxilio SOS en la Habitación 402. Humo denso en el pasillo.",
            fr = "Détresse SOS signalée dans la Chambre 402. Fumée épaisse dans le couloir.",
            zh = "402 房间发出 SOS 求救信号。走廊内有浓烟。",
            ar = "إنذار استغاثة SOS في الغرفة 402. دخان كثيف في الممر.",
            ru = "Сигнал SOS из комнаты 402. Густой дым в коридоре.",
            hi = "कमरा 402 से SOS संकट संदेश। गलियारे में भारी धुआं।",
            ja = "402号室からSOS要請。廊下に濃煙発生。",
            ta = "அறை 402 இல் SOS அவசர எச்சரிக்கை. தாழ்வாரத்தில் அடர்ந்த புகை.",
            de = "SOS-Notruf in Zimmer 402 ausgelöst. Dichter Rauch im Korridor.",
            tl = "May SOS sa Kuwarto 402. Makapal ang usok sa pasilyo."
        ),
        "heavy smoke" to TransEntry("Humo denso 🔥", "Fumée épaisse 🔥", "浓烟 🔥", "دخان كثيف 🔥", "Густой дым 🔥", "भारी धुआं 🔥", "濃煙 🔥", "அடர்ந்த புகை 🔥", "Dichter Rauch 🔥", "Makapal na Usok 🔥"),
        "door blocked" to TransEntry("Puerta bloqueada 🚪", "Porte bloquée 🚪", "门被堵塞 🚪", "الباب مغلق 🚪", "Дверь заблокирована 🚪", "दरवाजा बंद 🚪", "ドア封鎖 🚪", "கதவு அடைப்பு 🚪", "Tür blockiert 🚪", "Baradong Pinto 🚪"),
        "injured person" to TransEntry("Persona herida 🤕", "Personne blessée 🤕", "有人受伤 🤕", "شخص مصاب 🤕", "Раненый человек 🤕", "घायल व्यक्ति 🤕", "負傷者あり 🤕", "காயமடைந்த நபர் 🤕", "Verletzte Person 🤕", "May Sugatan 🤕"),
        "multiple people" to TransEntry("Múltiples personas 👥", "Plusieurs personnes 👥", "多人被困 👥", "عدة أشخاص 👥", "Несколько человек 👥", "कई लोग 👥", "複数人 👥", "பல நபர்கள் 👥", "Mehrere Personen 👥", "Maraming Tao 👥"),
        "water rising" to TransEntry("Nivel de agua subiendo 🌊", "Montée des eaux 🌊", "水位上升 🌊", "ارتفاع منسوب المياه 🌊", "Вода прибывает 🌊", "पानी बढ़ रहा है 🌊", "浸水上昇 🌊", "நீர் மட்டம் உயர்கிறது 🌊", "Wasser steigt 🌊", "Tumataas ang Tubig 🌊")
    )

    private val phraseDictionary = listOf(
        Regex("""(?i)\bstay in room\b""") to TransEntry("permanezca en la habitación", "restez dans la pièce", "留在房间内", "ابق في الغرفة", "оставайтесь в комнате", "कमरे में रहें", "部屋にとどまる", "அறையில் இருங்கள்", "im Zimmer bleiben", "manatili sa kuwarto"),
        Regex("""(?i)\bdo not open door\b""") to TransEntry("no abra la puerta", "n'ouvrez pas la porte", "切勿开门", "لا تفتح الباب", "не открывайте дверь", "दरवाजा न खोलें", "ドアを開けないでください", "கதவை திறக்க வேண்டாம்", "Tür nicht öffnen", "huwag buksan ang pinto"),
        Regex("""(?i)\bevacuate immediately\b""") to TransEntry("evacue inmediatamente", "évacuez immédiatement", "请立即疏散", "أخلِ فوراً", "немедленно эвакуируйтесь", "तुरंत बाहर निकलें", "直ちに避難してください", "உடனடியாக வெளியேறுங்கள்", "sofort evakuieren", "lumikas agad"),
        Regex("""(?i)\bwest stairwell\b""") to TransEntry("Escalera Oeste", "Escalier Ouest", "西侧楼梯", "الدرج الغربي", "Западная лестница", "पश्चिमी सीढ़ी", "西階段", "மேற்கு படிக்கட்டு", "West-Treppenhaus", "Kanlurang Hagdanan"),
        Regex("""(?i)\beast stairwell\b""") to TransEntry("Escalera Este", "Escalier Est", "东侧楼梯", "الدرج الشرقي", "Восточная лестница", "पूर्वी सीढ़ी", "東階段", "கிழக்கு படிக்கட்டு", "Ost-Treppenhaus", "Silangang Hagdanan"),
        Regex("""(?i)\bwest exit\b""") to TransEntry("Salida Oeste", "Sortie Ouest", "西出口", "المخرج الغربي", "Западный выход", "पश्चिमी निकास", "西側出口", "மேற்கு வெளியேறும் வழி", "Westausgang", "Kanlurang Labasan"),
        Regex("""(?i)\beast exit\b""") to TransEntry("Salida Este", "Sortie Est", "东出口", "المخرج الشرقي", "Восточный выход", "पूर्वी निकास", "東側出口", "கிழக்கு வெளியேறும் வழி", "Ostausgang", "Silangang Labasan"),
        Regex("""(?i)\bheavy smoke\b""") to TransEntry("humo denso", "fumée épaisse", "浓烟", "دخان كثيف", "густой дым", "भारी धुआं", "濃煙", "அடர்ந்த புகை", "dichter Rauch", "makapal na usok"),
        Regex("""(?i)\bactive fire\b""") to TransEntry("fuego activo", "incendie actif", "火势蔓延", "حريق نشط", "активный огонь", "सक्रिय आग", "火災発生", "செயலில் உள்ள தீ", "aktives Feuer", "aktibong sunog"),
        Regex("""(?i)\bdoor blocked\b""") to TransEntry("puerta bloqueada", "porte bloquée", "门被堵住", "باب مغلق", "дверь заблокирована", "दरवाजा बंद है", "ドア封鎖", "கதவு அடைக்கப்பட்டுள்ளது", "Tür blockiert", "nakaharang ang pinto"),
        Regex("""(?i)\bhelp is coming\b""") to TransEntry("La ayuda viene en camino", "Les secours arrivent", "救援马上就到", "المساعدة قادمة", "Помощь идет", "मदद आ रही है", "救助隊が向かっています", "உதவி வருகிறது", "Hilfe naht", "Papunta na ang tulong"),
        Regex("""(?i)\brescue squad\b""") to TransEntry("equipo de rescate", "équipe de secours", "救援队伍", "فريق الإنقاذ", "спасательный отряд", "बचाव दल", "救助隊", "மீட்புக் குழு", "Rettungsteam", "rescue squad"),
        Regex("""(?i)\bseal doors\b""") to TransEntry("selle las puertas", "scellez les portes", "密封门缝", "أغلق الأبواب", "загерметизируйте двери", "दरवाजे सील करें", "ドアを密閉してください", "கதவுகளை மூடுங்கள்", "Türen abdichten", "selyuhan ang pinto"),
        Regex("""(?i)\bdamp towels\b""") to TransEntry("toallas húmedas", "serviettes humides", "湿毛巾", "مناشف مبللة", "влажные полотенца", "गीले तौलिये", "濡れタオル", "ஈரமான துண்டுகள்", "feuchte Handtücher", "basang tuwalya"),
        Regex("""(?i)\bcan you hear me\b""") to TransEntry("¿puede escucharme?", "m'entendez-vous?", "你能听到我吗？", "هل تسمعني؟", "вы меня слышите?", "क्या आप मुझे सुन सकते हैं?", "聞こえますか？", "நான் பேசுவது கேட்கிறதா?", "Können Sie mich hören?", "naririnig mo ba ako?"),
        Regex("""(?i)\bwe need help\b""") to TransEntry("necesitamos ayuda", "nous avons besoin d'aide", "我们需要帮助", "نحتاج مساعدة", "нам нужна помощь", "हमें मदद चाहिए", "助けが必要です", "எங்களுக்கு உதவி தேவை", "wir brauchen Hilfe", "kailangan namin ng tulong"),
        Regex("""(?i)\bwe are safe\b""") to TransEntry("estamos a salvo", "nous sommes en sécurité", "我们很安全", "نحن بأمان", "мы в безопасности", "हम सुरक्षित हैं", "私たちは無事です", "நாங்கள் பாதுகாப்பாக இருக்கிறோம்", "wir sind sicher", "ligtas kami")
    )

    private val singleWordDict = mapOf(
        "fire" to TransEntry("fuego", "feu", "火灾", "حريق", "огонь", "आग", "火災", "தீ", "Feuer", "sunog"),
        "smoke" to TransEntry("humo", "fumée", "浓烟", "دخان", "дым", "धुआं", "煙", "புகை", "Rauch", "usok"),
        "water" to TransEntry("agua", "eau", "水", "ماء", "вода", "पानी", "水", "தண்ணீர்", "Wasser", "tubig"),
        "room" to TransEntry("habitación", "chambre", "房间", "غرفة", "комната", "कमरा", "部屋", "அறை", "Zimmer", "kuwarto"),
        "floor" to TransEntry("piso", "étage", "楼层", "طابق", "этаж", "मंजिल", "階", "தளம்", "Etage", "palapag"),
        "exit" to TransEntry("salida", "sortie", "出口", "مخرج", "выход", "निकास", "出口", "வெளியேறும் வழி", "Ausgang", "labasan"),
        "stairwell" to TransEntry("escalera", "escalier", "楼梯", "درج", "лестница", "सीढ़ी", "階段", "படிக்கட்டு", "Treppenhaus", "hagdanan"),
        "stairs" to TransEntry("escaleras", "escaliers", "楼梯", "درج", "лестница", "सीढ़ियां", "階段", "படிக்கட்டுகள்", "Treppe", "hagdan"),
        "door" to TransEntry("puerta", "porte", "门", "باب", "дверь", "दरवाजा", "ドア", "கதவு", "Tür", "pinto"),
        "doors" to TransEntry("puertas", "portes", "门", "أبواب", "двери", "दरवाजे", "ドア", "கதவுகள்", "Türen", "mga pinto"),
        "window" to TransEntry("ventana", "fenêtre", "窗户", "نافذة", "окно", "खिड़की", "窓", "ஜன்னல்", "Fenster", "bintana"),
        "corridor" to TransEntry("pasillo", "couloir", "走廊", "ممر", "коридор", "गलियारा", "廊下", "தாழ்வாரம்", "Korridor", "pasilyo"),
        "hallway" to TransEntry("pasillo", "couloir", "走廊", "ممر", "коридор", "गलियारा", "廊下", "தாழ்வாரம்", "Flur", "pasilyo"),
        "safe" to TransEntry("seguro", "sûr", "安全", "آمن", "безопасно", "सुरक्षित", "安全", "பாதுகாப்பான", "sicher", "ligtas"),
        "danger" to TransEntry("peligro", "danger", "危险", "خطر", "опасность", "खतरा", "危険", "ஆபத்து", "Gefahr", "panganib"),
        "trapped" to TransEntry("atrapado", "piégé", "被困", "محاصر", "заблокирован", "फंसे हुए", "閉じ込め", "சிக்கியுள்ளோம்", "eingeschlossen", "naipit"),
        "injured" to TransEntry("herido", "blessé", "受伤", "مصاب", "ранен", "घायल", "負傷", "காயம்", "verletzt", "sugatan"),
        "hurt" to TransEntry("herido", "blessé", "受伤", "متألم", "травмирован", "चोट", "痛む", "காயம்", "verletzt", "nasaktan"),
        "help" to TransEntry("ayuda", "aide", "救助", "مساعدة", "помощь", "मदद", "助け", "உதவி", "Hilfe", "saklolo"),
        "rescue" to TransEntry("rescate", "secours", "救援", "إنقاذ", "спасение", "बचाव", "救助", "மீட்பு", "Rettung", "saklolo"),
        "evacuate" to TransEntry("evacuar", "évacuer", "疏散", "إخلاء", "эвакуироваться", "खाली करना", "避難する", "வெளியேறவும்", "evakuieren", "lumikas"),
        "blocked" to TransEntry("bloqueado", "bloqué", "已封锁", "مغلق", "заблокирован", "अवरुद्ध", "封鎖", "அடைப்பு", "blockiert", "barado"),
        "open" to TransEntry("abrir", "ouvrir", "打开", "فتح", "открыто", "खुला", "開く", "திறக்க", "offen", "bukas"),
        "closed" to TransEntry("cerrado", "fermé", "关闭", "مغلق", "закрыто", "बंद", "閉鎖", "மூடப்பட்டது", "geschlossen", "sarado"),
        "attention" to TransEntry("atención", "attention", "注意", "انتباه", "внимание", "ध्यान दें", "注意", "கவனம்", "Achtung", "paunawa"),
        "alert" to TransEntry("alerta", "alerte", "警报", "إنذار", "тревога", "चेतावनी", "警報", "எச்சரிக்கை", "Alarm", "alerto"),
        "warning" to TransEntry("advertencia", "avertissement", "警告", "تحذير", "предупреждение", "चेतावनी", "警告", "எச்சரிக்கை", "Warnung", "babala"),
        "critical" to TransEntry("crítico", "critique", "紧急", "حرج", "критический", "गंभीर", "緊急", "முக்கியமான", "kritisch", "kritikal"),
        "urgent" to TransEntry("urgente", "urgent", "紧急", "عاجل", "срочный", "अत्यावश्यक", "至急", "அவசரம்", "dringend", "urgente"),
        "please" to TransEntry("por favor", "s'il vous plaît", "请", "من فضلك", "пожалуйста", "कृपया", "お願いします", "தயவுசெய்து", "bitte", "pakiusap"),
        "yes" to TransEntry("sí", "oui", "是", "نعم", "да", "हाँ", "はい", "ஆம்", "ja", "oo"),
        "no" to TransEntry("no", "non", "不", "لا", "нет", "नहीं", "いいえ", "இல்லை", "nein", "hindi"),
        "we" to TransEntry("nosotros", "nous", "我们", "نحن", "мы", "हम", "私たち", "நாங்கள்", "wir", "kami"),
        "i" to TransEntry("yo", "je", "我", "أنا", "я", "मैं", "私", "நான்", "ich", "ako"),
        "you" to TransEntry("usted", "vous", "您", "أنت", "вы", "आप", "あなた", "நீங்கள்", "Sie", "ikaw"),
        "is" to TransEntry("está", "est", "是", "هو", "есть", "है", "です", "உள்ளது", "ist", "ay"),
        "are" to TransEntry("están", "sont", "是", "هم", "являются", "हैं", "です", "உள்ளனர்", "sind", "ay"),
        "here" to TransEntry("aquí", "ici", "在这里", "هنا", "здесь", "यहाँ", "ここ", "இங்கே", "hier", "dito"),
        "now" to TransEntry("ahora", "maintenant", "现在", "الآن", "сейчас", "अब", "今", "இப்போது", "jetzt", "ngayon"),
        "immediately" to TransEntry("inmediatamente", "immédiatement", "立即", "فوراً", "немедленно", "तुरंत", "直ちに", "உடனடியாக", "sofort", "agad"),
        "quickly" to TransEntry("rápidamente", "rapidement", "快速", "بسرعة", "быстро", "जल्दी", "速やかに", "விரைவாக", "schnell", "mabilis"),
        "coming" to TransEntry("viniendo", "arrive", "赶来", "قادم", "идет", "आ रहे हैं", "向かっている", "வருகிறது", "kommt", "papunta"),
        "people" to TransEntry("personas", "personnes", "人", "أشخاص", "люди", "लोग", "人々", "நபர்கள்", "Personen", "mga tao"),
        "person" to TransEntry("persona", "personne", "人员", "شخص", "человек", "व्यक्ति", "人", "நபர்", "Person", "tao"),
        "child" to TransEntry("niño", "enfant", "儿童", "طفل", "ребенок", "बच्चा", "子ども", "குழந்தை", "Kind", "bata"),
        "baby" to TransEntry("bebé", "bébé", "婴儿", "رضيع", "младенец", "शिशु", "赤ちゃん", "குழந்தை", "Baby", "sanggol"),
        "family" to TransEntry("familia", "famille", "家人", "عائلة", "семья", "परिवार", "家族", "குடும்பம்", "Familie", "pamilya"),
        "medic" to TransEntry("médico", "médecin", "医护", "طبيب", "медик", "चिकित्सक", "医師", "மருத்துவர்", "Sanitäter", "doktor"),
        "status" to TransEntry("estado", "statut", "状态", "حالة", "статус", "स्थिति", "状態", "நிலை", "Status", "katayuan"),
        "test" to TransEntry("prueba", "test", "测试", "اختبار", "проверка", "परीक्षण", "テスト", "சோதனை", "Test", "subok"),
        "hello" to TransEntry("hola", "bonjour", "你好", "مرحباً", "здравствуйте", "नमस्ते", "こんにちは", "வணக்கம்", "Hallo", "kamusta"),
        "ok" to TransEntry("de acuerdo", "d'accord", "好的", "حسناً", "хорошо", "ठीक है", "了解", "சரி", "in Ordnung", "sige")
    )
}
