package com.ravex.civilization.npc;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword parser used when no AI is available. It is NOT AI and every reply it produces is prefixed with
 * "[offline]" by the caller so it can never be mistaken for real LLM output.
 */
public final class OfflineInterpreter {
    public record Result(String reply, String action, int count) {}

    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern STOP = Pattern.compile("(?iu)(stop|wait|halt|arr[êe]te|stoppe|attends|توقف|قف|استنى|إيقاف)");
    private static final Pattern PROTECT = Pattern.compile("(?iu)(protect|guard|defend|prot[èe]ge|d[ée]fends|احمني|احمي|حماية|دافع)");
    private static final Pattern WOOD = Pattern.compile("(?iu)(wood|logs?|trees?|bois|arbres?|b[uû]ches?|tronc|خشب|شجر|أشجار|اشجار)");
    private static final Pattern FOLLOW = Pattern.compile("(?iu)(follow|come|suis|viens|تعال|اتبعني|تبعني|إتبعني)");
    private static final Pattern UNSUPPORTED = Pattern.compile(
            "(?iu)(build|house|mine|iron|diamond|nether|dragon|craft|farm|construis|maison|fer|diamant|ابن|بيت|منزل|حديد|ألماس|الماس|بناء)");
    private static final Pattern GREETING = Pattern.compile("(?iu)(hello|hi|hey|salut|bonjour|coucou|مرحبا|أهلا|اهلا|سلام|السلام)");

    public static Result interpret(String text, String lang) {
        String t = text.toLowerCase(Locale.ROOT);
        Matcher num = NUMBER.matcher(t);
        int n = num.find() ? Integer.parseInt(num.group().length() > 4 ? "512" : num.group()) : 0;

        if (STOP.matcher(t).find())
            return new Result(Lang.t(lang, "Okay, stopping.", "D'accord, j'arrête.", "حسنًا، توقفت."), "stop", 0);
        if (PROTECT.matcher(t).find())
            return new Result(Lang.t(lang, "I'll stay close and protect you.", "Je reste près de toi et je te protège.",
                    "سأبقى قريبًا وأحميك."), "protect", 0);
        if (WOOD.matcher(t).find()) {
            int count = n > 0 ? n : 8;
            return new Result(String.format(Lang.t(lang, "On it - collecting %d logs.", "Je m'en occupe - je récolte %d bûches.",
                    "حاضر - سأجمع %d قطعة خشب."), count), "collect_wood", count);
        }
        if (FOLLOW.matcher(t).find())
            return new Result(Lang.t(lang, "Following you.", "Je te suis.", "أتبعك."), "follow", 0);
        if (UNSUPPORTED.matcher(t).find())
            return new Result(Lang.t(lang, "I can't do that yet - that skill isn't implemented in this version of the mod.",
                    "Je ne peux pas encore faire ça - cette capacité n'est pas encore implémentée dans cette version du mod.",
                    "لا أستطيع فعل ذلك بعد - هذه القدرة غير مُنفّذة في هذا الإصدار من المود."), "none", 0);
        if (GREETING.matcher(t).find())
            return new Result(Lang.t(lang, "Hello!", "Salut !", "مرحبًا!"), "none", 0);
        return new Result(Lang.t(lang,
                "I only understand basic commands without AI: follow, stop, collect wood, protect.",
                "Sans IA, je comprends seulement : suis-moi, stop, récolte du bois, protège-moi.",
                "بدون الذكاء الاصطناعي أفهم فقط: اتبعني، توقف، اجمع الخشب، احمني."), "none", 0);
    }

    private OfflineInterpreter() {}
}
