package com.example.java_gobang.game;



// 这个类表示一个 落子响应
public class AIGameResponse {
    private String message;
    private int p;
    private int pRow;
    private int pCol;
    private int e;
    private int eRow;
    private int eCol;
    private int winner;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public int getpRow() {
        return pRow;
    }

    public void setpRow(int pRow) {
        this.pRow = pRow;
    }

    public int getpCol() {
        return pCol;
    }

    public void setpCol(int pCol) {
        this.pCol = pCol;
    }

    public int getE() {
        return e;
    }

    public void setE(int e) {
        this.e = e;
    }

    public int geteRow() {
        return eRow;
    }

    public void seteRow(int eRow) {
        this.eRow = eRow;
    }

    public int geteCol() {
        return eCol;
    }

    public void seteCol(int eCol) {
        this.eCol = eCol;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }
}
