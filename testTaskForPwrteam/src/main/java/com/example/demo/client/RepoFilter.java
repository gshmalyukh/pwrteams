package com.example.demo.client;

public enum RepoFilter {
        FORKED,
        NONFORKED,
        ALL;
    public static RepoFilter fromString(String value) {
        switch (value.toLowerCase()) {
            case "forked":
                return FORKED;
            case "nonforked":
                return NONFORKED;
            case "all":
                return ALL;
            default:
                throw new IllegalArgumentException("Invalid filter value. Allowed values are: forked, nonforked, all.");
        }
    }
}
