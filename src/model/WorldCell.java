package model;

/** Содержимое клетки реального мира. Агенту этот объект передавать нельзя. */
public final class WorldCell {
    private boolean pit;
    private boolean wumpus;
    private boolean gold;

    public boolean hasPit() { return pit; }
    public boolean hasWumpus() { return wumpus; }
    public boolean hasGold() { return gold; }

    // Изменять содержимое могут только классы модели из этого пакета.
    void setPit(boolean pit) { this.pit = pit; }
    void setWumpus(boolean wumpus) { this.wumpus = wumpus; }
    void setGold(boolean gold) { this.gold = gold; }
}
