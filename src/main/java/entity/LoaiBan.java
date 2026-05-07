package entity;
public enum LoaiBan {
    TANG_TRET("TANG_TRET"),
    TANG_1("TANG_1"),
    PHONG("PHONG");
    private final String dbValue;
    LoaiBan(String dbValue) {
        this.dbValue = dbValue;
    }
    @Override
    public String toString() {
        return dbValue;
    }
    public static LoaiBan fromString(String text) {
        for (LoaiBan b : LoaiBan.values()) {
            if (b.dbValue.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null; // Hoặc ném IllegalArgumentException
    }
}