package com.college.lostfound.dto;

public class DashboardStats {

    private long totalUsers;
    private long totalLostItems;
    private long totalFoundItems;
    private long potentialMatches;
    private long pendingClaims;
    private long returnedItems;
    private long closedItems;

    public DashboardStats() {}

    public DashboardStats(long totalUsers, long totalLostItems, long totalFoundItems,
                          long potentialMatches, long pendingClaims, long returnedItems, long closedItems) {
        this.totalUsers = totalUsers;
        this.totalLostItems = totalLostItems;
        this.totalFoundItems = totalFoundItems;
        this.potentialMatches = potentialMatches;
        this.pendingClaims = pendingClaims;
        this.returnedItems = returnedItems;
        this.closedItems = closedItems;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalLostItems() { return totalLostItems; }
    public void setTotalLostItems(long totalLostItems) { this.totalLostItems = totalLostItems; }

    public long getTotalFoundItems() { return totalFoundItems; }
    public void setTotalFoundItems(long totalFoundItems) { this.totalFoundItems = totalFoundItems; }

    public long getPotentialMatches() { return potentialMatches; }
    public void setPotentialMatches(long potentialMatches) { this.potentialMatches = potentialMatches; }

    public long getPendingClaims() { return pendingClaims; }
    public void setPendingClaims(long pendingClaims) { this.pendingClaims = pendingClaims; }

    public long getReturnedItems() { return returnedItems; }
    public void setReturnedItems(long returnedItems) { this.returnedItems = returnedItems; }

    public long getClosedItems() { return closedItems; }
    public void setClosedItems(long closedItems) { this.closedItems = closedItems; }
}
