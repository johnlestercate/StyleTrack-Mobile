package com.styletrack.customer.data;

import java.util.ArrayList;
import java.util.List;

public class Page<T> {
    public List<T> content = new ArrayList<>();
    public long totalElements = 0;
}
