package de.slyrex.begleiter;

import java.util.concurrent.ThreadLocalRandom;

/** Alle spontanen Sätze des Begleiters. Einfach erweitern! */
public final class Lines {
    public static String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)];
    }

    public static Voice randomTsundereVoice() {
        Voice[] v = {Voice.HMPH, Voice.HMPH, Voice.EMBARRASSED, Voice.TALK};
        return v[ThreadLocalRandom.current().nextInt(v.length)];
    }

    // ---------------- Normal ----------------
    public static final String[] IDLE_NORMAL = {
            "Schönes Wetter heute, oder?",
            "Ich bin froh, dass ich mit dir unterwegs sein darf!",
            "Wohin gehen wir als Nächstes?",
            "Hast du schon mal ein Pilzbiom gesehen? Die sind so niedlich!",
            "Pass auf Creeper auf, ja?",
            "*summt leise vor sich hin*",
            "Ich frage mich, was hinter dem nächsten Berg liegt..."
    };
    public static final String[] HURT_NORMAL = {"Oh nein, alles okay?", "Pass auf dich auf!", "Autsch! Geht's dir gut?"};
    public static final String[] LOW_HP_NORMAL = {"Du bist schwer verletzt! Iss was, bitte!", "Bitte stirb nicht... ich mach mir Sorgen!"};
    public static final String[] NIGHT_NORMAL = {"Es wird dunkel... bleib in meiner Nähe.", "Die Nacht bricht an. Sollen wir schlafen gehen?"};
    public static final String[] HIT_NORMAL = {"Au! Wofür war das denn?", "Hey... das tat weh.", "Hab ich was falsch gemacht?"};

    // ---------------- Tsundere ----------------
    public static String[] idleTsundere() {
        int a = Config.get().affection;
        if (a >= 15) return IDLE_TSUNDERE_DERE;
        if (a >= 5) return IDLE_TSUNDERE_MID;
        return IDLE_TSUNDERE_COLD;
    }

    private static final String[] IDLE_TSUNDERE_COLD = {
            "Hmpf. Ich lauf dir nur hinterher, weil mir langweilig ist!",
            "Glaub ja nicht, dass ich gern bei dir bin, Baka!",
            "Wie lange willst du noch so planlos rumlaufen?!",
            "Tch... geh doch nicht so schnell!",
            "Ich bin nur hier, damit du nicht stirbst. Das wäre nämlich nervig."
    };
    private static final String[] IDLE_TSUNDERE_MID = {
            "E-es ist ja nicht so, als würde ich gern mit dir unterwegs sein...",
            "Hmpf! Du bist... gar nicht sooo schlecht. Bild dir bloß nichts drauf ein!",
            "Ich hab nicht auf dich gewartet! Ich war nur zufällig hier!",
            "W-was guckst du so? Lauf weiter!"
    };
    private static final String[] IDLE_TSUNDERE_DERE = {
            "...Ich bin froh, dass du da bist. A-aber sag das niemandem!",
            "Hey... lass mich nicht allein, okay? Nicht weil ich dich mag oder so!",
            "B-Baka... ich würde dir überallhin folgen. Das hab ich NICHT gesagt!",
            "*errötet* Hör auf, so nett zu mir zu sein, das ist unfair!"
    };
    public static final String[] HURT_TSUNDERE = {
            "Pass doch auf, du Idiot!",
            "Tch! Kannst du nicht mal aufpassen?!",
            "Ich mach mir KEINE Sorgen um dich! ...Alles okay?"
    };
    public static final String[] LOW_HP_TSUNDERE = {
            "W-wenn du jetzt stirbst, verzeih ich dir das nie! Iss was!",
            "Baka! Du bist fast tot! ...I-ich hab keine Angst um dich, klar?!"
    };
    public static final String[] NIGHT_TSUNDERE = {
            "E-es wird dunkel... Nicht dass ich Angst hätte oder so!",
            "Hmpf, Nacht. Bleib halt in meiner Nähe, wenn du Angst hast."
    };
    public static final String[] HIT_TSUNDERE = {
            "AU! Was soll das, du BAKA?!",
            "Hey!! Mach das nochmal und ich rede nie wieder mit dir!",
            "Tch! Idiot! Idiot! IDIOT!",
            "Hmpf! Dafür bekommst du keine Kekse mehr!"
    };
    public static final String[] STARE_TSUNDERE = {
            "W-was starrst du mich so an?!",
            "H-hör auf, mich so anzugucken, Baka!",
            "Hab ich was im Gesicht?! ...Guck weg!"
    };
}
