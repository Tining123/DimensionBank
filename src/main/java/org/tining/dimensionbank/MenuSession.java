package org.tining.dimensionbank;

public final class MenuSession {
    public int page = 1;
    public String filter = ""; // 小写匹配用

    public SortField sortField = SortField.AMOUNT;
    public SortDir sortDir = SortDir.DESC; // 默认数量↓
}
