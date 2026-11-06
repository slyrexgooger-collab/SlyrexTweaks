package de.slyrex.begleiter;

import de.slyrex.begleiter.screen.DialogueScreen;
import de.slyrex.begleiter.screen.DialogueScreen.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Der Dialogbaum. Jede Methode zeigt einen Text + Antwortmöglichkeiten. */
public final class Dialogues {
    private static long lastGiftTime = 0;
    private static long lastComplimentTime = 0;

    private static boolean t() {
        return Config.get().tsundere;
    }

    private static int aff() {
        return Config.get().affection;
    }

    private static String player() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null ? mc.player.getName().getString() : "du";
    }

    private static String pick(String... options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    // ------------------------------------------------------------ Hauptmenü

    public static void greet(DialogueScreen s) {
        String text;
        Voice v;
        if (t()) {
            if (aff() >= 15) {
                text = pick("O-oh, " + player() + "! Ich hab NICHT auf dich gewartet! ...Was gibt's?",
                        "Du schon wieder? *lächelt kurz* Ä-äh, ich meine: Hmpf! Was willst du?");
                v = Voice.EMBARRASSED;
            } else if (aff() >= 5) {
                text = pick("Was ist? Ich hab nicht ewig Zeit... aber gut, sag schon.",
                        "Hmpf. Du willst also mit mir reden? Na schön.");
                v = Voice.HMPH;
            } else {
                text = pick("Was willst DU denn?!", "Tch. Rede schnell, ich bin beschäftigt.",
                        "Hmpf! Glaub ja nicht, dass ich mich freue, dich zu sehen!");
                v = Voice.HMPH;
            }
        } else {
            text = pick("Hey " + player() + "! Schön, dass du mit mir redest!",
                    "Hallo! Was kann ich für dich tun?",
                    "Oh, hi! Ich hab gerade an dich gedacht.");
            v = Voice.HAPPY;
        }
        s.show(text, v, mainOptions(s));
    }

    public static void menu(DialogueScreen s) {
        String text = t() ? pick("Noch was?! ...Ich hör zu.", "Hmpf. Sonst noch was?", "W-was willst du noch?")
                : pick("Möchtest du noch über etwas reden?", "Noch etwas?", "Was liegt dir noch auf dem Herzen?");
        s.show(text, Voice.NONE, mainOptions(s));
    }

    private static List<Option> mainOptions(DialogueScreen s) {
        List<Option> o = new ArrayList<>();
        o.add(new Option("Wie geht's dir?", () -> howAreYou(s)));
        o.add(new Option("Erzähl mir was über dich.", () -> aboutYou(s)));
        o.add(new Option("Du siehst heute echt gut aus!", () -> compliment(s)));
        o.add(new Option("Magst du mich?", () -> doYouLikeMe(s)));
        o.add(new Option("Was sollen wir machen?", () -> whatToDo(s)));
        o.add(new Option("Hier, ein Geschenk!", () -> gift(s)));
        o.add(new Option(Config.get().following ? "Warte hier." : "Komm mit!", () -> toggleFollow(s)));
        o.add(new Option("Tschüss!", () -> bye(s)));
        return o;
    }

    private static List<Option> back(DialogueScreen s) {
        return List.of(new Option("Weiter...", () -> menu(s)), new Option("Tschüss!", () -> bye(s)));
    }

    // ------------------------------------------------------------ Themen

    private static void howAreYou(DialogueScreen s) {
        if (t()) {
            if (aff() >= 15) s.show("M-mir geht's gut... jetzt wo du fragst. N-nicht wegen dir!", Voice.EMBARRASSED, back(s));
            else s.show(pick("Wieso interessiert dich das?! ...Gut. Mir geht's gut. Zufrieden?",
                    "Hmpf. Ging mir besser, bevor du mich das gefragt hast."), Voice.HMPH, back(s));
        } else {
            s.show(pick("Mir geht's super! Abenteuer mit dir machen einfach Spaß.",
                    "Gut, danke! Und dir? Ich hoffe, du passt gut auf dich auf."), Voice.HAPPY, back(s));
        }
    }

    private static void aboutYou(DialogueScreen s) {
        String name = Config.get().displayName;
        if (t()) {
            s.show(pick("Ich bin " + name + ". Mehr musst du nicht wissen! ...Okay, ich mag Kekse. Und Katzen. SAG ES NIEMANDEM!",
                    "Was gibt's da zu erzählen? Ich bin halt toll. Im Gegensatz zu dir, Baka.",
                    "Früher bin ich allein durch die Welt gezogen. Das war... e-egal! Jetzt bin ich halt hier."),
                    Voice.HMPH, List.of(
                            new Option("Du magst Katzen? Süß!", () -> s.show("S-SÜSS?! Ich bin nicht süß! *wird rot*", Voice.EMBARRASSED, back(s))),
                            new Option("Okay...", () -> menu(s))));
        } else {
            s.show(pick("Ich heiße " + name + "! Ich liebe es, neue Biome zu entdecken und Blumen zu sammeln.",
                    "Ich bin " + name + ". Bevor ich dich getroffen habe, war ich ziemlich allein. Danke, dass du da bist!"),
                    Voice.HAPPY, back(s));
        }
    }

    private static void compliment(DialogueScreen s) {
        long now = System.currentTimeMillis();
        boolean counts = now - lastComplimentTime > 60_000;
        if (counts) {
            lastComplimentTime = now;
            Config.addAffection(1);
        }
        if (t()) {
            s.show(pick("W-W-WAS?! D-das weiß ich selber, Baka! *wird knallrot*",
                    "H-hör auf so Sachen zu sagen! ...Findest du wirklich?",
                    "Hmpf! Schleimer! ...D-danke."), Voice.EMBARRASSED, back(s));
        } else {
            s.show(pick("Aww, danke! Du aber auch!", "Hehe, das ist lieb von dir!"), Voice.HAPPY, back(s));
        }
    }

    private static void doYouLikeMe(DialogueScreen s) {
        int a = aff();
        if (t()) {
            if (a >= 20) s.show("...J-ja. Okay?! Ich mag dich. Sehr sogar. Z-zufrieden?! Und jetzt schau mich nicht so an!", Voice.EMBARRASSED, back(s));
            else if (a >= 10) s.show("W-was ist das denn für eine Frage?! N-nein! ...Vielleicht ein kleines bisschen. NEIN!", Voice.EMBARRASSED, back(s));
            else if (a >= 0) s.show("Dich mögen? HA! Träum weiter, Baka!", Voice.HMPH, back(s));
            else s.show("Nach allem, was du getan hast?! Nein! Hmpf!", Voice.ANGRY, back(s));
        } else {
            if (a >= 10) s.show("Natürlich! Du bist mein liebster Mensch auf der ganzen Welt!", Voice.HAPPY, back(s));
            else if (a >= 0) s.show("Ja klar! Wir sind doch Freunde.", Voice.HAPPY, back(s));
            else s.show("Ich... weiß nicht. Du warst nicht immer nett zu mir.", Voice.SAD, back(s));
        }
    }

    private static void whatToDo(DialogueScreen s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long time = mc.world != null ? mc.world.getTimeOfDay() % 24000L : 0;
        boolean night = time >= 13000 && time < 23000;
        String idea;
        if (night) {
            idea = t() ? "Es ist Nacht, du Genie. Bau ein Bett. N-nicht für uns zusammen oder so!"
                    : "Es ist Nacht – lass uns schlafen gehen oder ein sicheres Lager bauen!";
        } else {
            idea = t() ? pick("Diamanten suchen. Und wehe, du gibst mir keinen ab!",
                    "Lass uns ein Haus bauen. Ein schönes! Nicht so eine Dreckhütte wie deine letzte.",
                    "Wie wär's, wenn du mal Kekse backst? Für... für dich natürlich. Nicht für mich.")
                    : pick("Wie wär's mit einer Höhlenerkundung?",
                    "Lass uns ein Dorf suchen!",
                    "Wir könnten einen Garten anlegen – mit ganz vielen Blumen!");
        }
        s.show(idea, t() ? Voice.HMPH : Voice.HAPPY, back(s));
    }

    private static void gift(DialogueScreen s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ItemStack stack = mc.player != null ? mc.player.getMainHandStack() : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            s.show(t() ? "Deine Hand ist leer, Baka! Willst du mich veräppeln?!"
                    : "Hm? Du hast ja gar nichts in der Hand. Halte ein Item in der Hand!", t() ? Voice.ANGRY : Voice.TALK, back(s));
            return;
        }

        long now = System.currentTimeMillis();
        boolean counts = now - lastGiftTime > 5 * 60_000; // nur alle 5 Minuten zählt ein Geschenk
        String item = stack.getName().getString();

        if (stack.isIn(ItemTags.SMALL_FLOWERS) || stack.isOf(Items.SUNFLOWER) || stack.isOf(Items.ROSE_BUSH)
                || stack.isOf(Items.PEONY) || stack.isOf(Items.LILAC)) {
            if (counts) Config.addAffection(3);
            s.show(t() ? "E-eine " + item + "?! F-für mich? ...Ich nehm sie nur, damit sie nicht verwelkt! *drückt sie fest an sich*"
                    : "Oh, eine " + item + "! Die ist wunderschön, danke!", t() ? Voice.EMBARRASSED : Voice.HAPPY, back(s));
        } else if (stack.isOf(Items.COOKIE) || stack.isOf(Items.CAKE) || stack.isOf(Items.PUMPKIN_PIE)
                || stack.isOf(Items.SWEET_BERRIES) || stack.isOf(Items.HONEY_BOTTLE) || stack.isOf(Items.GLOW_BERRIES)) {
            if (counts) Config.addAffection(4);
            s.show(t() ? "S-Süßes?! ...Ich hatte eh gerade Hunger. Das ist KEIN Zeichen, dass ich dich mag! *mampf*"
                    : "Mmmh, " + item + "! Das ist mein Lieblingsessen!", t() ? Voice.EMBARRASSED : Voice.HAPPY, back(s));
        } else if (stack.isOf(Items.DIAMOND) || stack.isOf(Items.EMERALD) || stack.isOf(Items.GOLD_INGOT)
                || stack.isOf(Items.AMETHYST_SHARD) || stack.isOf(Items.NETHERITE_INGOT)) {
            if (counts) Config.addAffection(5);
            s.show(t() ? "W-w-was?! " + item + "?! Du... du Idiot, das ist doch viel zu wertvoll! ...Ich behalte es trotzdem."
                    : "Wow, " + item + "! Das ist ja ein richtiger Schatz! Danke!!", t() ? Voice.EMBARRASSED : Voice.HAPPY, back(s));
        } else if (stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.SPIDER_EYE) || stack.isOf(Items.POISONOUS_POTATO)
                || stack.isOf(Items.DIRT) || stack.isOf(Items.BONE)) {
            if (counts) Config.addAffection(-2);
            s.show(t() ? "IIIH! " + item + "?! Bist du verrückt?! BAKA!" : "Ähm... " + item + "? Danke... glaub ich?",
                    t() ? Voice.ANGRY : Voice.SAD, back(s));
        } else {
            if (counts) Config.addAffection(1);
            s.show(t() ? "Hmpf. " + item + ". Naja... besser als nichts. D-danke."
                    : "Oh, " + item + "! Danke, das ist nett!", t() ? Voice.HMPH : Voice.HAPPY, back(s));
        }
        if (counts) lastGiftTime = now;
    }

    private static void toggleFollow(DialogueScreen s) {
        Config c = Config.get();
        c.following = !c.following;
        Config.save();
        if (c.following) {
            s.show(t() ? "Hmpf, na gut, ich komm mit. Aber nur weil du ohne mich verloren bist!" : "Juhu, los geht's!",
                    t() ? Voice.HMPH : Voice.HAPPY, back(s));
        } else {
            s.show(t() ? "Ich soll hier WARTEN?! ...Na schön. Aber komm ja zurück, Baka!" : "Okay, ich warte hier auf dich!",
                    t() ? Voice.HMPH : Voice.TALK, back(s));
        }
    }

    private static void bye(DialogueScreen s) {
        String text = t() ? (aff() >= 15 ? "B-bis gleich... Ich vermisse dich NICHT!" : pick("Endlich! Hmpf.", "Ja ja, geh schon."))
                : pick("Bis später!", "Tschüss! Ich bin immer hier, wenn du mich brauchst.");
        s.close();
        CompanionManager.say(text, t() ? Voice.HMPH : Voice.HAPPY);
    }
}
