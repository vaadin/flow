package hummingbird.component;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Component;
import com.vaadin.ui.HTML;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Slider;
import com.vaadin.ui.VerticalLayout;

public class GridUI extends AbstractTestUIWithLog {

    Grid grid = new Grid();

    @Override
    protected void setup(VaadinRequest request) {
        Slider s = new Slider();
        s.setPin(true);
        s.setMin(0);
        s.setMax(3);
        s.setStep(1);
        s.addValueChangeListener(e -> {
            int count = s.getValue().intValue();
            grid.setFrozenColumnCount(count);
            log("Set frozen columns to " + count);
        });

        Slider s2 = new Slider();
        s2.setPin(true);
        s2.setMin(0);
        s2.setMax(10);
        s2.setStep(1);
        s2.addValueChangeListener(e -> {
            int count = s2.getValue().intValue();
            grid.setHeightByRows(count);
            log("Set rows to " + count);
        });
        s2.setValue(5.0);

        HorizontalLayout hl = new HorizontalLayout(caption(s, "Frozen columns"),
                caption(s2, "Number of rows to show"));
        add(hl);

        grid.addColumn("N<u>am</u>e").setExpandRatio(1); // .setHidable(true);
        grid.addColumn("Hero Name"); // .setHidable(true);
        grid.addColumn("Arch enemy").setMinimumWidth(300); // .setHidable(true)

        grid.addRow("Joonas Lehtinen", "The Nuclear Veteran",
                "The Righteous Swordsman");
        grid.addRow("Jurka Rahikkala", "The Elegent Gunner",
                "The Electric Smasher");
        grid.addRow("Jani Laakso", "The Aqua Fighter", "The Hypnotic Mole");
        grid.addRow("Marc Englund", "The Swift Whiz", "The Iron Spectacle");
        grid.addRow("Sami Ekblad", "Master Clever Wolverine",
                "Light Spectacle");
        grid.addRow("Henri Muurimaa", "Cool Fighter",
                "Master Royal Mastermind");
        grid.addRow("Ville Ingman", "Professor Dark Genius",
                "Mister Kind Shade");
        grid.addRow("Mikael Vappula", "Godly Wasp",
                "Warden Gentle Grasshopper");
        grid.addRow("Tomi Virtanen", "Voiceless Stranger",
                "The Mysterious Nightowl");
        grid.addRow("Artur Signell", "Silver Goliath",
                "The Impossible Starling");
        grid.addRow("Jouni Koivuviita", "The Jolly Ibis", "The Dark Prince");
        grid.addRow("Matti Tahvonen", "The Storm Snipe",
                "The Magnificent Axeman");
        grid.addRow("Jonatan Kronqvist", "The Brass Wolf", "Starbright");
        grid.addRow("Hannu Salonen", "The Majestic Seer", "Deadnite");
        grid.addRow("Henri Kerola", "Master Long Slayer", "Kind Magician");
        grid.addRow("Teemu Pöntelin", "Earth Hawk", "Gigantic Mothman");
        grid.addRow("Johannes Tuikkala", "Purple Mothman", "Fiery Shepherd");
        grid.addRow("Risto Yrjänä", "Agent Accidental Wasp",
                "Professor Golden Scepter");
        grid.addRow("Kim Leppänen", "Thornhead", "The Gray Bear");
        grid.addRow("Jonas Granvik", "Death Roach", "The Honorable Sparrow");
        grid.addRow("Jens Jansson", "The Old Phoenix", "The Fabulous Eagle");
        grid.addRow("Henrik Paul", "The Outrageous Bear", "The Brave Spirit");
        grid.addRow("John Ahlroos", "The Hypnotic Warden", "Solar Flare");
        grid.addRow("Thomas Mattsson", "The Ancient Sentinel", "Mister Y");
        grid.addRow("Fredrik Rönnlund", "Bronze Gloom", "Lord Terrific Spider");
        grid.addRow("Teppo Kurki", "Agent Gentle Cheetah", "Brass Tiger");
        grid.addRow("Henri Sara", "Accidental Daggers", "Honorable Slayer");
        grid.addRow("Sebastian Nyholm", "Doctor Infamous Grasshopper",
                "Warden Marked Prophet");
        grid.addRow("Peter Lehto", "Ice Raven", "The Quick Monarch");
        grid.addRow("Tomi Virkki", "The Green Scout", "The Golden Angel");
        grid.addRow("Mikael Grankvist", "The Fire Hammer",
                "The Glorious Monarch");
        grid.addRow("Petter Holmström", "The Nuclear Phoenix",
                "The Aqua Doctor");
        grid.addRow("Marcus Hellberg", "The Copper Watcher", "Mister Penance");
        grid.addRow("Petri Heinonen", "Orange Guardian", "Red Fighter");
        grid.addRow("Matti Vesa", "Cold Dagger", "Doctor Steel Owl");
        grid.addRow("Anna Koskinen", "Doctor Fantastic Wasp",
                "Master Silver Fighter");
        grid.addRow("Marlon Richert", "Silver Protector",
                "Lord Impossible Wasp");
        grid.addRow("Jarno Rantala", "Fallen Phoenix", "The Mammoth Champion");
        grid.addRow("Marko Grönroos", "Cleanser", "The Dramatic Watcher");
        grid.addRow("Johannes Häyry", "The Royal Sparrow",
                "The Vengeful Hammer");
        grid.addRow("Leif Åstrand", "The Vengeful Hammer", "The Royal Sparrow");
        grid.addRow("Sami Kaksonen", "The Dramatic Watcher", "Cleanser");
        grid.addRow("Johannes Dahlström", "The Mammoth Champion",
                "Fallen Phoenix");
        grid.addRow("Samuli Penttilä", "Lord Impossible Wasp",
                "Silver Protector");
        grid.addRow("Rolf Smeds", "Master Silver Fighter",
                "Doctor Fantastic Wasp");
        grid.addRow("Sami Viitanen", "Doctor Steel Owl", "Cold Dagger");
        grid.addRow("Pekka Hyvönen", "Red Fighter", "Orange Guardian");
        grid.addRow("Tapio Aali", "Mister Penance", "The Copper Watcher");
        grid.addRow("Haijian Wang", "The Aqua Doctor", "The Nuclear Phoenix");
        grid.addRow("Mikolaj Olszewski", "The Glorious Monarch",
                "The Fire Hammer");
        grid.addRow("Tanja Repo", "The Golden Angel", "The Green Scout");
        grid.addRow("Pekka Perälä", "The Quick Monarch", "Ice Raven");
        grid.addRow("Jonni Nakari", "Warden Marked Prophet",
                "Doctor Infamous Grasshopper");
        grid.addRow("Denis Anisimov", "Honorable Slayer", "Accidental Daggers");
        grid.addRow("Amir Al-Majdalawi", "Brass Tiger", "Agent Gentle Cheetah");
        grid.addRow("Patrik Lindström", "Lord Terrific Spider", "Bronze Gloom");
        grid.addRow("Michael Tzukanov", "Mister Y", "The Ancient Sentinel");
        grid.addRow("Matti Hosio", "Solar Flare", "The Hypnotic Warden");
        grid.addRow("Juho Nurminen", "The Brave Spirit", "The Outrageous Bear");
        grid.addRow("Johannes Eriksson", "The Fabulous Eagle",
                "The Old Phoenix");
        grid.addRow("Joacim Päivärinne", "The Honorable Sparrow",
                "Death Roach");
        grid.addRow("Felype Ferreira", "The Gray Bear", "Thornhead");
        grid.addRow("Mika Murtojärvi", "Professor Golden Scepter",
                "Agent Accidental Wasp");
        grid.addRow("Artem Godin", "Fiery Shepherd", "Purple Mothman");
        grid.addRow("Maciej Przepiora", "Gigantic Mothman", "Earth Hawk");
        grid.addRow("Teemu Suo-Anttila", "Kind Magician", "Master Long Slayer");
        grid.addRow("Minna Hohti", "Deadnite", "The Majestic Seer");
        grid.addRow("Olli Helttula", "Starbright", "The Brass Wolf");
        grid.addRow("Sauli Tähkäpää", "The Magnificent Axeman",
                "The Storm Snipe");
        grid.addRow("Manuel Carrasco", "The Dark Prince", "The Jolly Ibis");
        grid.addRow("Jarmo Kemppainen", "The Impossible Starling",
                "Silver Goliath");
        grid.addRow("Juuso Valli", "The Mysterious Nightowl",
                "Voiceless Stranger");
        grid.addRow("Henrik Skogström", "Warden Gentle Grasshopper",
                "Godly Wasp");
        grid.addRow("Dmitrii Rogozin", "Mister Kind Shade",
                "Professor Dark Genius");
        grid.addRow("Markus Koivisto", "Master Royal Mastermind",
                "Cool Fighter");
        grid.addRow("Bogdan Udrescu", "Light Spectacle",
                "Master Clever Wolverine");
        grid.addRow("Heikki Ohinmaa", "The Iron Spectacle", "The Swift Whiz");
        grid.addRow("Guillermo Alvarez", "The Hypnotic Mole",
                "The Aqua Fighter");
        grid.addRow("Sergey Budkin", "The Electric Smasher",
                "The Elegent Gunner");
        grid.addRow("Kari Kaitanen", "The Righteous Swordsman",
                "The Nuclear Veteran");

        add(grid);
    }

    private Component caption(Component c, String caption) {
        return new VerticalLayout(new HTML(caption), c);
    }

}
