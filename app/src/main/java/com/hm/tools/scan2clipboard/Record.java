package com.hm.tools.scan2clipboard;

import java.util.ArrayList;

/**
 * Created by huang on 10/26/16.
 */

public class Record {
    ArrayList<String> results;
    int success = 0;
    int miss = 0;
    int fail = 0;
    int count = 0;
    int total;

    public Record(int total) {
        this.total = total;
        results = new ArrayList<>();
    }

    public ArrayList<String> getResults() {
        return results;
    }

    public int getSuccess() {
        return success;
    }

    public int getMiss() {
        return miss;
    }

    public int getFail() {
        return fail;
    }

    public int getCount() {
        return count;
    }

    public int getTotal() {
        return total;
    }

    public void setResults(ArrayList<String> results) {
        this.results = results;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public void setMiss(int miss) {
        this.miss = miss;
    }

    public void setFail(int fail) {
        this.fail = fail;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
