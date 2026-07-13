package damagetracker.tracker;

public class DamageRecord {
    public enum DamageType {
        DIRECT,
        POISON,
        VULNERABLE_CONTRIBUTION,
        WEAK_REDUCTION,
        STRENGTH_CONTRIBUTION
    }

    private final String sourcePlayerId;
    private final String targetName;
    private final int amount;
    private final DamageType type;
    private final String detail;
    private final int turn;

    public DamageRecord(String sourcePlayerId, String targetName, int amount,
                        DamageType type, String detail, int turn) {
        this.sourcePlayerId = sourcePlayerId;
        this.targetName = targetName;
        this.amount = amount;
        this.type = type;
        this.detail = detail;
        this.turn = turn;
    }

    public String getSourcePlayerId() { return sourcePlayerId; }
    public String getTargetName() { return targetName; }
    public int getAmount() { return amount; }
    public DamageType getType() { return type; }
    public String getDetail() { return detail; }
    public int getTurn() { return turn; }
}
