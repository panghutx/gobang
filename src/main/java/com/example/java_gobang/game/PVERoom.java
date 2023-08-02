package com.example.java_gobang.game;

import com.example.java_gobang.model.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

// 这个类就表示一个游戏房间
public class PVERoom {
    private String userId;
    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;
    // 这个二维数组用来表示棋盘
    // 约定:
    // 1) 使用 0 表示当前位置未落子. 初始化好的 int 二维数组, 就相当于是 全 0
    // 2) 使用 1 表示 user1 的落子位置
    // 3) 使用 2 表示 user2 的落子位置
    private int[][] board = new int[MAX_ROW][MAX_COL];

    // 创建 ObjectMapper 用来转换 JSON
    private ObjectMapper objectMapper = new ObjectMapper();


    private UserMapper userMapper;

    // 通过这个方法来处理一次落子操作.
    // 要做的事情:
    public void putChess(String reqJson,WebSocketSession session) throws IOException {
        // 1. 记录当前落子的位置.
        GameRequest request = objectMapper.readValue(reqJson, GameRequest.class);
        AIGameResponse response = new AIGameResponse();

        int row = request.getRow();
        int col = request.getCol();

        if (board[row][col] != 0) {
            // 在客户端已经针对重复落子进行过判定了. 此处为了程序更加稳健, 在服务器再判定一次.
            System.out.println("当前位置 (" + row + ", " + col + ") 已经有子了!");
            return;
        }
        board[row][col] = 1;
        System.out.println("row:"+row+" col:"+col+board[row][col]);
        // 2. 打印出当前的棋盘信息, 方便来观察局势. 也方便后面验证胜负关系的判定.

//        printBoard();

        // 3. 进行胜负判定  1-玩家  2-机器人
        int winner = checkWinner(row, col, 1);
        if(winner!=0){
            response.setMessage("putChess");
            response.setP(1);
            response.setpRow(row);
            response.setWinner(winner);
            String respJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(respJson));
            return;
        }
        //TODO 机器人下棋
        int[] ai = AIpush();
        int erow =ai[0];
        int ecol=ai[1];
        System.out.println("row:"+erow+" col:"+ecol+board[erow][ecol]);
        board[erow][ecol] = 2;
        System.out.println("row:"+erow+" col:"+ecol+board[erow][ecol]);


        // 3. 进行胜负判定  1-玩家  2-机器人
        winner = checkWinner(erow, ecol, 2);
        // 4. 给房间中的所有客户端都返回响应.
        response.setMessage("putChess");
        response.setP(1);
        response.setpRow(row);
        response.setpCol(col);
        response.setE(2);
        response.seteRow(erow);
        response.seteCol(ecol);
        response.setWinner(winner);

        // 把响应构造成 JSON 字符串, 通过 session 进行传输.
        String respJson = objectMapper.writeValueAsString(response);

        session.sendMessage(new TextMessage(respJson));

    }

    private int[] AIpush() {
        int[] ret = concluate();
        return ret;
    }
    //根据评分表来分配分数, my表示电脑下的棋子, his表示他下的棋子
    public int score(int my,int his){
        if(my > 5) return 200000;
        if(my == 5 && his == 0) return 200000;
        if(my == 5 && his == 1) return 200000;
        if(my == 5 && his == 2) return 200000;
        if(my == 4 && his == 1) return 3000;
        if(my == 4 && his == 0) return 50000;
        if(my == 4 && his == 2) return 1000;
        if(my == 3 && his == 0) return 3000;
        if(my == 3 && his == 1) return 1000;
        if(my == 3 && his == 2) return 500;
        if(my == 2 && his == 0) return 500;
        if(my == 2 && his == 1) return 200;
        if(my == 2 && his == 2) return 100;
        if(my == 1 && his == 0) return 100;
        if(my == 1 && his == 1) return 50;
        if(my == 1 && his == 2) return 30;
        return 0;
    }

    //横向得分
    public int getXScore(int x,int y, int chess){
        int my = 1;
        int his = 0;
        for(int i = x-1; i >= 0; i--){
            if(chess == board[i][y]){
                my++;
            }else if(board[i][y] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        for(int i = x+1; i<board.length; i++) {
            if(chess == board[i][y]){
                my++;
            }else if(board[i][y] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        return score(my,his);
    }

    //纵向得分
    private int getYScore(int x, int y, int chess) {
        int my = 1;
        int his = 0;
        for(int i = y-1; i >= 0; i--){
            if(chess == board[x][i]){
                my++;
            }else if(board[x][i] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        for(int i = y+1; i < board.length; i++){
            if(chess == board[x][i]){
                my++;
            }else if(board[x][i] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        return score(my,his);
    }

    //左斜对角线得分
    private int getSkewScore2(int x, int y, int chess) {
        int my = 1;
        int his = 0;
        for(int i = x+1,j=y-1; i<board.length && j >=0; i++,j--){
            if(chess == board[i][j]){
                my++;
            }else if(board[i][j] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        for(int i = x-1,j=y+1; i>=0 && j<board.length; i--,j++){
            if(chess == board[i][j]){
                my++;
            }else if(board[i][j] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        return score(my,his);
    }

    //右斜对角线得分
    private int getSkewScore1(int x, int y, int chess) {
        int my = 1;
        int his = 0;
        for(int i = x-1,j =y-1; i >=0 && j>=0; i--,j--){
            if(chess == board[i][j]){
                my++;
            }else if(board[i][j] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        for(int i = x+1,j=y+1; j<board.length && i < board.length; i++,j++){
            if(chess == board[i][j]){
                my++;
            }else if(board[i][j] == 0){
                break;
            }else{
                his++;
                break;
            }
        }
        return score(my,his);
    }

    //棋子总得分
    public int getScore(int x,int y) {
        int numX1 = getXScore(x,y,1);
        int numX2 = getXScore(x,y,2);
        int numY1 = getYScore(x,y,1);
        int numY2 = getYScore(x,y,2);
        int skew1 = getSkewScore1(x,y,1);
        int skew2 = getSkewScore1(x,y,2);
        int skew3 = getSkewScore2(x,y,1);
        int skew4 = getSkewScore2(x,y,2);
        if(numX2 >= 200000 || numY2 >= 200000 || skew2 >= 200000 || skew4 >= 200000) {
            return Integer.MAX_VALUE;
        }
        if(numX1 >= 200000 || numY1 >= 200000 || skew1 >= 200000 || skew3 >= 200000){
            return Integer.MAX_VALUE;
        }
        int xScore = getXScore(x,y,1)+getXScore(x,y,2);
        int yScore = getYScore(x,y,1)+getYScore(x,y,2);
        int skewScore1 = getSkewScore1(x,y,1)+getSkewScore1(x,y,2);
        int skewScore2 = getSkewScore2(x,y,1)+getSkewScore2(x,y,2);
        return xScore + yScore + skewScore1 + skewScore2;
    }

    //确认棋子位置
    public int[] concluate() {
        int[] res = new int[2];
        int max = 0;
        for(int i = 0; i < MAX_ROW; i++) {
            for(int j = 0; j < MAX_COL; j++) {
                if(board[i][j] != 0) {
                    continue;
                }
                int num = getScore(i,j);
                if(num == 200000){
                    res[0] = i;
                    res[1] = j;
                    return res;
                }
                if(num > max) {
                    max = num;
                    res[0] = i;
                    res[1] = j;
                }
            }
        }

        return res;
    }


    private void printBoard() {
        // 打印出棋盘
        System.out.println("[打印棋盘信息] " );
        System.out.println("=====================================================================");
        for (int r = 0; r < MAX_ROW; r++) {
            for (int c = 0; c < MAX_COL; c++) {
                // 针对一行之内的若干列, 不要打印换行
                System.out.print(board[r][c] + " ");
            }
            // 每次遍历完一行之后, 再打印换行.
            System.out.println();
        }
        System.out.println("=====================================================================");
    }

    // 使用这个方法来判定当前落子是否分出胜负.
    // 1为玩家，2为机器
    // 如果胜负未分, 就返回 0
    private int checkWinner(int row, int col, int chess) {
        // 1. 检查所有的行
        //    先遍历这五种情况
        for (int c = col - 4; c <= col; c++) {
            // 针对其中的一种情况, 来判定这五个子是不是连在一起了~
            // 不光是这五个子得连着, 而且还得和玩家落的子是一样~~ (才算是获胜)
            try {
                if (board[row][c] == chess
                        && board[row][c + 1] == chess
                        && board[row][c + 2] == chess
                        && board[row][c + 3] == chess
                        && board[row][c + 4] == chess) {
                    // 构成了五子连珠! 胜负已分!
                    return chess == 1 ? 1 : 2;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果出现数组下标越界的情况, 就在这里直接忽略这个异常.
                continue;
            }
        }

        // 2. 检查所有列
        for (int r = row - 4; r <= row; r++) {
            try {
                if (board[r][col] == chess
                        && board[r + 1][col] == chess
                        && board[r + 2][col] == chess
                        && board[r + 3][col] == chess
                        && board[r + 4][col] == chess) {
                    return chess == 1 ? 1 : 2;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        // 3. 检查左对角线
        for (int r = row - 4, c = col - 4; r <= row && c <= col; r++, c++) {
            try {
                if (board[r][c] == chess
                        && board[r + 1][c + 1] == chess
                        && board[r + 2][c + 2] == chess
                        && board[r + 3][c + 3] == chess
                        && board[r + 4][c + 4] == chess) {
                    return chess == 1 ? 1 : 2;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        // 4. 检查右对角线
        for (int r = row - 4, c = col + 4; r <= row && c >= col; r++, c--) {
            try {
                if (board[r][c] == chess
                        && board[r + 1][c - 1] == chess
                        && board[r + 2][c - 2] == chess
                        && board[r + 3][c - 3] == chess
                        && board[r + 4][c - 4] == chess) {
                    return chess == 1 ? 1 : 2;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        // 胜负未分, 就直接返回 0 了.
        return 0;
    }


    public PVERoom() {

    }

    public static void main(String[] args) {
        Room room = new Room();
        System.out.println(room);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
