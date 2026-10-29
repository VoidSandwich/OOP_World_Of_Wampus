package model;

/** Результат команды для контроллера и журнала. */
public final class ActionResult {
    private final boolean successful;
    private final String message;
    private final boolean hunterDied;
    private final String deathReason;
    private final boolean bumped;
    private final boolean screamed;
    private final boolean goldCollected;
    private final boolean climbed;

    public ActionResult(boolean successful, String message, boolean hunterDied,
                        String deathReason, boolean bumped, boolean screamed,
                        boolean goldCollected, boolean climbed) {
        this.successful = successful;
        this.message = message == null ? "" : message;
        this.hunterDied = hunterDied;
        this.deathReason = deathReason == null ? "" : deathReason;
        this.bumped = bumped;
        this.screamed = screamed;
        this.goldCollected = goldCollected;
        this.climbed = climbed;
    }
    public static ActionResult rejected(String message) {
        return new ActionResult(false, message, false, "", false, false, false, false);
    }
    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public boolean hasHunterDied() { return hunterDied; }
    public String getDeathReason() { return deathReason; }
    public boolean hasBumped() { return bumped; }
    public boolean hasScreamed() { return screamed; }
    public boolean hasCollectedGold() { return goldCollected; }
    public boolean hasClimbed() { return climbed; }
    @Override public String toString() { return message; }
}
