package model;

import java.util.ArrayList;
import java.util.List;

/** Показания датчиков охотника за один момент времени. */
public final class Percepts {
    private final boolean breeze;
    private final boolean stench;
    private final boolean glitter;
    private final boolean bump;
    private final boolean scream;

    public Percepts(boolean breeze, boolean stench, boolean glitter,
                    boolean bump, boolean scream) {
        this.breeze = breeze;
        this.stench = stench;
        this.glitter = glitter;
        this.bump = bump;
        this.scream = scream;
    }

    public static Percepts none() {
        return new Percepts(false, false, false, false, false);
    }

    public boolean hasBreeze() { return breeze; }
    public boolean hasStench() { return stench; }
    public boolean hasGlitter() { return glitter; }
    public boolean hasBump() { return bump; }
    public boolean hasScream() { return scream; }

    /** Возвращает краткое русское описание для журнала/панели датчиков. */
    public String toRussianText() {
        List<String> values = new ArrayList<>();
        if (breeze) values.add("Ветерок");
        if (stench) values.add("Запах");
        if (glitter) values.add("Блеск");
        if (bump) values.add("Удар");
        if (scream) values.add("Крик");
        return values.isEmpty() ? "Нет сигналов" : String.join(", ", values);
    }

    @Override
    public String toString() {
        return "Percepts{breeze=" + breeze
                + ", stench=" + stench
                + ", glitter=" + glitter
                + ", bump=" + bump
                + ", scream=" + scream + '}';
    }
}
